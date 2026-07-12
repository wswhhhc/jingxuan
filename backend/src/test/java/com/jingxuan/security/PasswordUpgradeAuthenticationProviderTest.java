package com.jingxuan.security;

import com.jingxuan.BaseServiceTest;
import com.jingxuan.config.SecurityConfig;
import com.jingxuan.entity.SysRole;
import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.RoleEnum;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.mapper.SysMenuMapper;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.mapper.SysUserMapper;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("DaoAuthenticationProvider - BCrypt 哈希透明升级")
class PasswordUpgradeAuthenticationProviderTest extends BaseServiceTest {

    private static final Long USER_ID = 42L;
    private static final String USERNAME = "legacy-student";
    private static final String RAW_PASSWORD = "Legacy84Password";

    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysRoleMapper sysRoleMapper;
    @Mock private SysMenuMapper sysMenuMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final AtomicReference<String> databasePassword = new AtomicReference<>();
    private final AtomicReference<UserDetails> passwordServiceResult = new AtomicReference<>();
    private final AtomicBoolean replacePasswordBeforeCas = new AtomicBoolean();
    private final List<PasswordUpgradeAttempt> upgradeAttempts = new ArrayList<>();

    private CustomUserDetailsService userDetailsService;
    private String concurrentPassword;

    @BeforeEach
    void setUp() {
        userDetailsService = new RecordingCustomUserDetailsService(
                sysUserMapper, sysRoleMapper, sysMenuMapper, passwordServiceResult);

        SysRole role = new SysRole();
        role.setId((long) RoleEnum.STUDENT.getValue());
        role.setRoleCode(RoleEnum.STUDENT.getAuthority());
        role.setRoleName(RoleEnum.STUDENT.getLabel());

        when(sysRoleMapper.selectById((long) RoleEnum.STUDENT.getValue())).thenReturn(role);
        when(sysUserMapper.selectPermissionsByUserId(USER_ID)).thenReturn(List.of());
        when(sysUserMapper.findByUsername(USERNAME))
                .thenAnswer(invocation -> enabledUser(databasePassword.get()));
        when(sysUserMapper.upgradePasswordIfUnchanged(
                        anyLong(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    Long userId = invocation.getArgument(0);
                    String expectedPassword = invocation.getArgument(1);
                    String newPassword = invocation.getArgument(2);

                    if (replacePasswordBeforeCas.compareAndSet(true, false)) {
                        databasePassword.set(concurrentPassword);
                    }

                    int updated = databasePassword.compareAndSet(expectedPassword, newPassword)
                            ? 1 : 0;
                    upgradeAttempts.add(new PasswordUpgradeAttempt(
                            userId, expectedPassword, newPassword, updated));
                    return updated;
                });
    }

    @Test
    @DisplayName("真实 Provider 登录把 cost 10 哈希升级为 cost 12")
    void shouldUpgradeCostTenHashThroughAuthenticationProvider() {
        String legacyHash = new BCryptPasswordEncoder(10).encode(RAW_PASSWORD);
        databasePassword.set(legacyHash);

        Authentication authentication = authenticate(RAW_PASSWORD);

        assertTrue(authentication.isAuthenticated());
        PasswordUpgradeAttempt attempt = onlyUpgradeAttempt();
        assertEquals(USER_ID, attempt.userId());
        assertEquals(legacyHash, attempt.expectedPassword());
        assertEquals(1, attempt.updatedRows());
        assertEquals(attempt.newPassword(), databasePassword.get());
        assertTrue(attempt.newPassword().matches("^\\$2[aby]\\$12\\$.*"));
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, attempt.newPassword()));
        assertFalse(passwordEncoder.upgradeEncoding(attempt.newPassword()));

        JwtUserDetails updatedUser = assertInstanceOf(
                JwtUserDetails.class, passwordServiceResult.get());
        assertEquals(attempt.newPassword(), updatedUser.getPassword());
    }

    @Test
    @DisplayName("真实 Provider 登录 cost 12 用户时不写密码")
    void shouldNotWriteWhenHashAlreadyUsesCostTwelve() {
        String currentHash = passwordEncoder.encode(RAW_PASSWORD);
        databasePassword.set(currentHash);

        Authentication authentication = authenticate(RAW_PASSWORD);

        assertTrue(authentication.isAuthenticated());
        assertTrue(upgradeAttempts.isEmpty());
        assertEquals(currentHash, databasePassword.get());
    }

    @Test
    @DisplayName("真实 Provider 拒绝错误密码且不写密码")
    void shouldNotWriteWhenPasswordIsWrong() {
        String legacyHash = new BCryptPasswordEncoder(10).encode(RAW_PASSWORD);
        databasePassword.set(legacyHash);

        assertThrows(BadCredentialsException.class,
                () -> authenticate("Wrong84Password"));

        assertTrue(upgradeAttempts.isEmpty());
        assertEquals(legacyHash, databasePassword.get());
    }

    @Test
    @DisplayName("认证快照旧哈希与并发新密码不匹配时 CAS=0 且不覆盖新密码")
    void shouldPreserveConcurrentPasswordWhenAuthenticationSnapshotIsStale() {
        String authenticationSnapshot = new BCryptPasswordEncoder(10)
                .encode(RAW_PASSWORD);
        String concurrentHash = new BCryptPasswordEncoder(10)
                .encode("Concurrent93Password");
        databasePassword.set(authenticationSnapshot);
        concurrentPassword = concurrentHash;
        replacePasswordBeforeCas.set(true);

        Authentication authentication = authenticate(RAW_PASSWORD);

        assertTrue(authentication.isAuthenticated());
        PasswordUpgradeAttempt attempt = onlyUpgradeAttempt();
        assertEquals(authenticationSnapshot, attempt.expectedPassword());
        assertEquals(0, attempt.updatedRows());
        assertEquals(concurrentHash, databasePassword.get());
    }

    @Test
    @DisplayName("Mapper CAS 对旧哈希使用二进制比较并保留毫秒更新时间")
    void mapperCasShouldUseBinaryHashComparisonAndMillisecondTimestamp() throws Exception {
        Method method = SysUserMapper.class.getMethod(
                "upgradePasswordIfUnchanged", Long.class, String.class, String.class);
        Update update = method.getAnnotation(Update.class);
        String sql = String.join(" ", update.value()).replaceAll("\\s+", " ");

        assertTrue(sql.contains("BINARY password = BINARY #{expectedPassword}"));
        assertTrue(sql.contains("update_time = CURRENT_TIMESTAMP(3)"));
        assertTrue(sql.contains("deleted = 0"));
    }

    private Authentication authenticate(String rawPassword) {
        return authenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken(USERNAME, rawPassword));
    }

    private ProviderManager authenticationManager() {
        try {
            Method factory = SecurityConfig.class.getDeclaredMethod(
                    "authenticationManager",
                    CustomUserDetailsService.class,
                    PasswordEncoder.class);
            SecurityConfig securityConfig = new SecurityConfig(null, null, null, null);
            AuthenticationManager manager = (AuthenticationManager) factory.invoke(
                    securityConfig, userDetailsService, passwordEncoder);

            ProviderManager providerManager = assertInstanceOf(
                    ProviderManager.class, manager);
            assertEquals(1, providerManager.getProviders().size());
            assertInstanceOf(DaoAuthenticationProvider.class,
                    providerManager.getProviders().get(0));
            providerManager.setEraseCredentialsAfterAuthentication(false);
            return providerManager;
        } catch (ReflectiveOperationException exception) {
            return fail("SecurityConfig 必须显式构造并返回 DaoAuthenticationProvider/ProviderManager",
                    exception);
        }
    }

    private SysUser enabledUser(String passwordHash) {
        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setUsername(USERNAME);
        user.setRealName("历史用户");
        user.setRoleId(RoleEnum.STUDENT.getValue());
        user.setStatus(UserStatusEnum.ENABLED);
        user.setFirstLogin(false);
        user.setPassword(passwordHash);
        return user;
    }

    private PasswordUpgradeAttempt onlyUpgradeAttempt() {
        assertEquals(1, upgradeAttempts.size());
        return upgradeAttempts.get(0);
    }

    private record PasswordUpgradeAttempt(
            Long userId,
            String expectedPassword,
            String newPassword,
            int updatedRows) {
    }

    private static final class RecordingCustomUserDetailsService
            extends CustomUserDetailsService {

        private final AtomicReference<UserDetails> passwordServiceResult;

        private RecordingCustomUserDetailsService(
                SysUserMapper sysUserMapper,
                SysRoleMapper sysRoleMapper,
                SysMenuMapper sysMenuMapper,
                AtomicReference<UserDetails> passwordServiceResult) {
            super(sysUserMapper, sysRoleMapper, sysMenuMapper);
            this.passwordServiceResult = passwordServiceResult;
        }

        @Override
        public UserDetails updatePassword(UserDetails user, String newPassword) {
            UserDetails updatedUser = super.updatePassword(user, newPassword);
            passwordServiceResult.set(updatedUser);
            return updatedUser;
        }
    }
}

package com.jingxuan.security;

import com.jingxuan.BaseServiceTest;
import com.jingxuan.entity.SysRole;
import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.RoleEnum;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@DisplayName("DaoAuthenticationProvider - BCrypt 哈希透明升级")
class PasswordUpgradeAuthenticationProviderTest extends BaseServiceTest {

    private static final Long USER_ID = 42L;
    private static final String USERNAME = "legacy-student";
    private static final String RAW_PASSWORD = "Legacy84Password";

    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysRoleMapper sysRoleMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @BeforeEach
    void setUp() {
        SysRole role = new SysRole();
        role.setId((long) RoleEnum.STUDENT.getValue());
        role.setRoleCode(RoleEnum.STUDENT.getAuthority());
        role.setRoleName(RoleEnum.STUDENT.getLabel());
        when(sysRoleMapper.selectById((long) RoleEnum.STUDENT.getValue())).thenReturn(role);
        when(sysUserMapper.selectPermissionsByUserId(USER_ID)).thenReturn(List.of());
    }

    @Test
    @DisplayName("登录成功且角色匹配返回认证身份")
    void shouldAuthenticateWithCorrectCredentials() {
        String hash = passwordEncoder.encode(RAW_PASSWORD);
        when(sysUserMapper.findByUsername(USERNAME)).thenReturn(enabledUser(hash));

        Authentication authentication = authenticate(RAW_PASSWORD);
        assertTrue(authentication.isAuthenticated());
        JwtUserDetails details = assertInstanceOf(JwtUserDetails.class, authentication.getPrincipal());
        assertEquals(USER_ID.longValue(), details.getUserId());
        assertEquals(USERNAME, details.getUsername());
    }

    private Authentication authenticate(String rawPassword) {
        CustomUserDetailsService userDetailsService =
                new CustomUserDetailsService(sysUserMapper, sysRoleMapper);
        return authenticationManager(userDetailsService).authenticate(
                new UsernamePasswordAuthenticationToken(USERNAME, rawPassword));
    }

    private ProviderManager authenticationManager(CustomUserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return new ProviderManager(provider);
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
}


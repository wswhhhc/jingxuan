package com.jingxuan.service;

import com.jingxuan.entity.SysUser;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SysUserServiceImpl - 用户创建密码安全")
class SysUserServiceImplTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private PasswordEncoder passwordEncoder;

    private SysUserServiceImpl sysUserService;

    @BeforeEach
    void setUp() {
        sysUserService = new SysUserServiceImpl(sysUserMapper, passwordEncoder);
        ReflectionTestUtils.setField(sysUserService, "baseMapper", sysUserMapper);
    }

    @Test
    @DisplayName("单个创建缺少初始密码时拒绝保存")
    void shouldRejectMissingInitialPassword() {
        SysUser user = userWithPassword(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.createUser(user));

        assertEquals("初始密码不能为空", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    @DisplayName("批量创建中的空白初始密码同样拒绝保存")
    void shouldRejectBlankInitialPasswordUsedByBatchCreation() {
        SysUser user = userWithPassword("   ");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.createUser(user));

        assertEquals("初始密码不能为空", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    @DisplayName("显式初始密码只以编码结果保存")
    void shouldEncodeExplicitInitialPasswordBeforeSaving() {
        SysUser user = userWithPassword("Cedar!84Wave");
        when(passwordEncoder.encode("Cedar!84Wave")).thenReturn("$2a$12$encoded-value");
        when(sysUserMapper.insert(any(SysUser.class))).thenReturn(1);

        boolean created = sysUserService.createUser(user);

        assertTrue(created);
        assertEquals("$2a$12$encoded-value", user.getPassword());
        verify(passwordEncoder).encode("Cedar!84Wave");
        verify(sysUserMapper).insert(user);
    }

    @Test
    @DisplayName("少于 8 个 UTF-8 字节的初始密码拒绝保存")
    void shouldRejectInitialPasswordShorterThanEightBytes() {
        SysUser user = userWithPassword("abc123");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.createUser(user));

        assertEquals("初始密码长度必须为8-72个UTF-8字节", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    @DisplayName("超过 BCrypt 72 字节上限的初始密码拒绝保存")
    void shouldRejectInitialPasswordLongerThanSeventyTwoUtf8Bytes() {
        SysUser user = userWithPassword("密".repeat(24) + "a1");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.createUser(user));

        assertEquals("初始密码长度必须为8-72个UTF-8字节", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("初始密码必须同时包含字母和数字")
    void shouldRejectInitialPasswordWithoutLetterAndDigit() {
        SysUser user = userWithPassword("abcdefgh");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.createUser(user));

        assertEquals("初始密码必须同时包含字母和数字", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("非十进制数字字符不满足数字要求")
    void shouldRejectNonDecimalNumberCharacters() {
        SysUser user = userWithPassword("Abcdefg²");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.createUser(user));

        assertEquals("初始密码必须同时包含字母和数字", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("管理员重置密码时应用相同安全策略")
    void shouldApplySamePolicyWhenResettingPassword() {
        SysUser existing = userWithPassword("$2a$10$legacy-hash");
        existing.setId(7L);
        when(sysUserMapper.selectById(7L)).thenReturn(existing);

        SysUser update = new SysUser();
        update.setId(7L);
        update.setPassword("abc123");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.updateUser(update));

        assertEquals("新密码长度必须为8-72个UTF-8字节", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("管理员更新传入空白密码时保持原密码不变")
    void shouldIgnoreBlankPasswordWhenUpdatingUser() {
        SysUser existing = userWithPassword("$2a$10$legacy-hash");
        existing.setId(7L);
        when(sysUserMapper.selectById(7L)).thenReturn(existing);
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        SysUser update = new SysUser();
        update.setId(7L);
        update.setPassword("   ");

        assertTrue(sysUserService.updateUser(update));

        assertNull(update.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(sysUserMapper).updateById(update);
    }

    @Test
    @DisplayName("初始密码包含孤立 Unicode 代理项时拒绝保存")
    void shouldRejectMalformedUnicodeInInitialPassword() {
        String malformedPassword = "A1" + String.valueOf((char) 0xD800).repeat(24);
        SysUser user = userWithPassword(malformedPassword);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.createUser(user));

        assertEquals("初始密码包含无效Unicode字符", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    private SysUser userWithPassword(String password) {
        SysUser user = new SysUser();
        user.setUsername("secure-student-01");
        user.setRealName("安全测试学生");
        user.setRoleId(1);
        user.setPassword(password);
        return user;
    }
}

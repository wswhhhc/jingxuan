package com.jingxuan.auth.service;

import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {

    @Test
    void teacherRegistrationCreatesPendingApprovalAccount() {
        SysUserMapper users = mock(SysUserMapper.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("jingxuan:verify:teacher@example.edu:2")).thenReturn("123456");
        when(users.countByUsername("teacher001")).thenReturn(0);
        when(users.countByEmailAndRole("teacher@example.edu", 2)).thenReturn(0);
        when(passwords.encode("ExamplePass2026")).thenReturn("bcrypt12");
        RegistrationService service = new RegistrationService(users, passwords, redis, emptyMailSenderProvider());

        service.register(Map.of(
                "username", "teacher001",
                "password", "ExamplePass2026",
                "realName", "教师",
                "email", "teacher@example.edu",
                "verifyCode", "123456",
                "roleId", 2));

        ArgumentCaptor<SysUser> captured = ArgumentCaptor.forClass(SysUser.class);
        verify(users).insert(captured.capture());
        assertEquals(UserStatusEnum.PENDING_APPROVAL, captured.getValue().getStatus());
    }

    @Test
    void rejectsAlreadyConsumedOrWrongVerificationCodeBeforeInsertingAUser() {
        SysUserMapper users = mock(SysUserMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(users.countByUsername("teacher001")).thenReturn(0);
        when(users.countByEmailAndRole("teacher@example.edu", 2)).thenReturn(0);

        RegistrationService service = new RegistrationService(users, mock(PasswordEncoder.class), redis,
                emptyMailSenderProvider());

        assertThrows(com.jingxuan.exception.BusinessException.class, () -> service.register(Map.of(
                "username", "teacher001", "password", "ExamplePass2026", "realName", "教师",
                "email", "teacher@example.edu", "verifyCode", "123456", "roleId", 2)));

        verify(users, never()).insert(any(SysUser.class));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> emptyMailSenderProvider() {
        return mock(ObjectProvider.class);
    }
}

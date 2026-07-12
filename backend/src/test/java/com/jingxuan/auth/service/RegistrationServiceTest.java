package com.jingxuan.auth.service;

import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
        when(redis.execute(any(RedisScript.class), eq(java.util.List.of("jingxuan:verify:teacher@example.edu:2")), eq("123456")))
                .thenReturn(1L);
        when(users.countByUsername("teacher001")).thenReturn(0);
        when(users.countByEmailAndRole("teacher@example.edu", 2)).thenReturn(0);
        when(passwords.encode("ExamplePass2026")).thenReturn("bcrypt12");
        RegistrationService service = new RegistrationService(users, passwords, redis, mock(JavaMailSender.class));

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
        verify(redis).execute(any(RedisScript.class), eq(java.util.List.of("jingxuan:verify:teacher@example.edu:2")), eq("123456"));
    }

    @Test
    void rejectsAlreadyConsumedOrWrongVerificationCodeBeforeInsertingAUser() {
        SysUserMapper users = mock(SysUserMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(users.countByUsername("teacher001")).thenReturn(0);
        when(users.countByEmailAndRole("teacher@example.edu", 2)).thenReturn(0);
        when(redis.execute(any(RedisScript.class), eq(java.util.List.of("jingxuan:verify:teacher@example.edu:2")), eq("123456")))
                .thenReturn(0L);
        RegistrationService service = new RegistrationService(users, mock(PasswordEncoder.class), redis,
                mock(JavaMailSender.class));

        org.junit.jupiter.api.Assertions.assertThrows(com.jingxuan.exception.BusinessException.class, () -> service.register(Map.of(
                "username", "teacher001", "password", "ExamplePass2026", "realName", "教师",
                "email", "teacher@example.edu", "verifyCode", "123456", "roleId", 2)));

        org.mockito.Mockito.verify(users, org.mockito.Mockito.never()).insert(any(SysUser.class));
    }
}

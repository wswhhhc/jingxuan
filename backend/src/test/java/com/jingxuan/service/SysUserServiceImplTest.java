package com.jingxuan.service;

import com.jingxuan.entity.SysUser;
import com.jingxuan.mapper.SysUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SysUserServiceImpl - 用户创建")
class SysUserServiceImplTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("创建用户时调用 encode 并插入数据库")
    void shouldEncodePasswordAndInsert() {
        when(passwordEncoder.encode("test-pass-01")).thenReturn("$2a$12$encoded-hash");

        SysUserServiceImpl service = new SysUserServiceImpl(sysUserMapper, passwordEncoder);
        ReflectionTestUtils.setField(service, "baseMapper", sysUserMapper);

        SysUser user = new SysUser();
        user.setUsername("student-01");
        user.setPassword("test-pass-01");

        service.createUser(user);

        verify(passwordEncoder).encode("test-pass-01");
        verify(sysUserMapper).insert((SysUser) any());
    }
}

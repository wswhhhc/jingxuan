package com.jingxuan.security;

import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.mapper.SysMenuMapper;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceApprovalTest {

    @Test
    void pendingApprovalTeacherCannotAuthenticate() {
        SysUserMapper users = mock(SysUserMapper.class);
        SysUser teacher = new SysUser();
        teacher.setUsername("teacher001");
        teacher.setStatus(UserStatusEnum.PENDING_APPROVAL);
        when(users.findByUsername("teacher001")).thenReturn(teacher);
        CustomUserDetailsService service = new CustomUserDetailsService(
                users, mock(SysRoleMapper.class), mock(SysMenuMapper.class));

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("teacher001"));
    }
}

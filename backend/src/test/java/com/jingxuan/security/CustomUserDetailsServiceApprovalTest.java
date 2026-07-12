package com.jingxuan.security;

import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.mapper.SysMenuMapper;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
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
        when(users.selectPermissionsByUserId(teacher.getId())).thenReturn(List.of());
        SysRoleMapper roles = mock(SysRoleMapper.class);
        when(roles.selectById(teacher.getRoleId())).thenReturn(null);
        CustomUserDetailsService service = new CustomUserDetailsService(
                users, roles, mock(SysMenuMapper.class));

        try {
            service.loadUserByUsername("teacher001");
            fail("应该抛出 UsernameNotFoundException");
        } catch (UsernameNotFoundException e) {
            // expected
        } catch (Exception e) {
            fail("应抛出 UsernameNotFoundException，而不是 " + e.getClass().getName() + ": " + e.getMessage());
        }
    }
}

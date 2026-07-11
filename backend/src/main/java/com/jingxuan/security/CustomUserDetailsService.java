package com.jingxuan.security;

import com.jingxuan.entity.SysRole;
import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.mapper.SysMenuMapper;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 加载用户认证信息
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserMapper.findByUsername(username);
        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (sysUser.getStatus() == UserStatusEnum.DISABLED) {
            throw new UsernameNotFoundException("账号已被禁用");
        }

        SysRole role = sysUser.getRoleId() != null
                ? sysRoleMapper.selectById(sysUser.getRoleId().longValue())
                : null;
        String roleCode = role != null ? role.getRoleCode() : "ROLE_STUDENT";

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(roleCode));

        // 加载菜单权限标识
        List<String> permissions = sysUserMapper.selectPermissionsByUserId(sysUser.getId());
        permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));

        return new JwtUserDetails(
                sysUser.getId(),
                sysUser.getUsername(),
                sysUser.getRealName(),
                sysUser.getPassword(),
                sysUser.getRoleId(),
                roleCode,
                authorities
        );
    }
}

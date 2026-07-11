package com.jingxuan.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 自定义 UserDetails — 额外携带 userId、realName、roleCode
 */
@Getter
public class JwtUserDetails extends User {

    private final Long userId;
    private final String realName;
    private final String roleCode;
    private final Integer roleId;

    public JwtUserDetails(Long userId, String username, String realName, String password,
                          Integer roleId, String roleCode,
                          Collection<? extends GrantedAuthority> authorities) {
        super(username, password, true, true, true, true, authorities);
        this.userId = userId;
        this.realName = realName;
        this.roleCode = roleCode;
        this.roleId = roleId;
    }
}

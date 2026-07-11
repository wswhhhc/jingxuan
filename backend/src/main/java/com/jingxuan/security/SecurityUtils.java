package com.jingxuan.security;

import com.jingxuan.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类 — 从 SecurityContext 获取当前登录用户信息
 * 依赖 JwtAuthenticationFilter 预先将 JwtUserDetails 设入上下文
 */
@Slf4j
public class SecurityUtils {

    private SecurityUtils() {}

    /**
     * 获取当前用户 ID
     */
    public static Long getCurrentUserId() {
        JwtUserDetails details = getJwtUserDetails();
        return details != null ? details.getUserId() : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        JwtUserDetails details = getJwtUserDetails();
        return details != null ? details.getUsername() : null;
    }

    /**
     * 获取当前用户真实姓名
     */
    public static String getCurrentRealName() {
        JwtUserDetails details = getJwtUserDetails();
        return details != null ? details.getRealName() : null;
    }

    /**
     * 获取当前用户角色编码
     */
    public static String getCurrentRoleCode() {
        JwtUserDetails details = getJwtUserDetails();
        return details != null ? details.getRoleCode() : null;
    }

    /**
     * 获取当前用户角色 ID
     */
    public static Integer getCurrentRoleId() {
        JwtUserDetails details = getJwtUserDetails();
        return details != null ? details.getRoleId() : null;
    }

    /**
     * 是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }

    /**
     * 是否拥有指定角色
     */
    public static boolean hasRole(String roleCode) {
        JwtUserDetails details = getJwtUserDetails();
        return details != null && roleCode.equals(details.getRoleCode());
    }

    /**
     * 强制获取当前用户 ID，不存在则抛异常
     */
    public static Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException(401, "未登录或登录已过期");
        }
        return userId;
    }

    private static JwtUserDetails getJwtUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtUserDetails details) {
            return details;
        }
        return null;
    }
}

package com.jingxuan.security;

import com.jingxuan.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecurityUtils 单元测试
 *
 * 覆盖：已认证/未认证/匿名用户/角色判断/强制获取用户ID
 */
@DisplayName("SecurityUtils")
class SecurityUtilsTest {

    private static final Long TEST_USER_ID = 10001L;
    private static final String TEST_USERNAME = "zhangsan";
    private static final String TEST_REAL_NAME = "张三";
    private static final String TEST_ROLE_CODE = "admin";
    private static final Integer TEST_ROLE_ID = 3;

    @BeforeEach
    void setUp() {
        // 无认证上下文 —— clean state
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(JwtUserDetails details) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                details, null, List.of(new SimpleGrantedAuthority("ROLE_" + details.getRoleCode())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private JwtUserDetails createUserDetails() {
        return new JwtUserDetails(
                TEST_USER_ID, TEST_USERNAME, TEST_REAL_NAME, "password",
                TEST_ROLE_ID, TEST_ROLE_CODE,
                List.of(new SimpleGrantedAuthority("ROLE_" + TEST_ROLE_CODE)));
    }

    // ===== 未认证场景 =====

    @Test
    @DisplayName("无认证上下文时返回 null/false")
    void withoutAuth_shouldReturnNull() {
        assertNull(SecurityUtils.getCurrentUserId());
        assertNull(SecurityUtils.getCurrentUsername());
        assertNull(SecurityUtils.getCurrentRealName());
        assertNull(SecurityUtils.getCurrentRoleCode());
        assertNull(SecurityUtils.getCurrentRoleId());
        assertFalse(SecurityUtils.isAuthenticated());
        assertFalse(SecurityUtils.hasRole("admin"));
    }

    // ===== 已认证场景 =====

    @Test
    @DisplayName("已认证时返回正确的用户信息")
    void withAuth_shouldReturnUserInfo() {
        setAuthentication(createUserDetails());

        assertEquals(TEST_USER_ID, SecurityUtils.getCurrentUserId());
        assertEquals(TEST_USERNAME, SecurityUtils.getCurrentUsername());
        assertEquals(TEST_REAL_NAME, SecurityUtils.getCurrentRealName());
        assertEquals(TEST_ROLE_CODE, SecurityUtils.getCurrentRoleCode());
        assertEquals(TEST_ROLE_ID, SecurityUtils.getCurrentRoleId());
        assertTrue(SecurityUtils.isAuthenticated());
    }

    @Test
    @DisplayName("hasRole 应匹配角色编码")
    void hasRole_shouldMatchRoleCode() {
        setAuthentication(createUserDetails());
        assertTrue(SecurityUtils.hasRole("admin"));
        assertFalse(SecurityUtils.hasRole("teacher"));
        assertFalse(SecurityUtils.hasRole("student"));
    }

    // ===== requireCurrentUserId =====

    @Test
    @DisplayName("requireCurrentUserId 已认证时返回用户 ID")
    void requireCurrentUserId_withAuth_shouldReturnUserId() {
        setAuthentication(createUserDetails());
        assertEquals(TEST_USER_ID, SecurityUtils.requireCurrentUserId());
    }

    @Test
    @DisplayName("requireCurrentUserId 未认证时应抛出 UnauthorizedException")
    void requireCurrentUserId_withoutAuth_shouldThrow() {
        assertThrows(UnauthorizedException.class, SecurityUtils::requireCurrentUserId);
    }

    // ===== 匿名用户边界 =====

    @Test
    @DisplayName("匿名用户应被视为未认证")
    void anonymousUser_shouldNotBeAuthenticated() {
        Authentication anonymous = new UsernamePasswordAuthenticationToken(
                "anonymousUser", null, List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertFalse(SecurityUtils.isAuthenticated());
        assertNull(SecurityUtils.getCurrentUserId());
        assertNull(SecurityUtils.getCurrentRoleCode());
    }
}

package com.jingxuan.security;

import com.jingxuan.exception.UnauthorizedException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtTokenProvider 单元测试
 *
 * 覆盖：生成 → 解析 → 验证全链路，过期/无效/空 Token 边界
 */
@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    /**
     * 符合 HMAC-SHA 要求的 Base64 密钥（256 bit）
     */
    private static final String TEST_SECRET = "dGhpcyBpcyBhIHRlc3Qgc2VjcmV0IGtleSBmb3Igand0IDI1NiBiaXRzIGxvbmc=";
    private static final long TEST_EXPIRATION = 86400000L; // 24h
    private static final long TEST_REMEMBER_EXPIRATION = 604800000L; // 7d

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        provider = new JwtTokenProvider(TEST_SECRET);
        // 反射设值：单元测试中 @Value 注入不生效
        setField(provider, "expiration", TEST_EXPIRATION);
        setField(provider, "rememberExpiration", TEST_REMEMBER_EXPIRATION);
    }

    private void setField(Object target, String fieldName, long value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("构造时传入空密钥应抛出异常")
    void constructor_shouldRejectBlankSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(""));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider("   "));
    }

    @Test
    @DisplayName("生成 Token → 解析出正确的用户信息")
    void generateToken_shouldEncodeUserClaims() {
        String token = provider.generateToken(10001L, "zhangsan", "1", false);

        assertNotNull(token);
        assertTrue(token.startsWith("eyJ")); // JWT 标准开头

        assertEquals(10001L, provider.getUserIdFromToken(token));
        assertEquals("zhangsan", provider.getUsernameFromToken(token));
        assertEquals("1", provider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("记住密码模式应生成更长有效期的 Token")
    void generateToken_withRememberMe_shouldHaveLongerExpiration() throws Exception {
        String normalToken = provider.generateToken(1L, "u1", "1", false);
        String rememberToken = provider.generateToken(1L, "u1", "1", true);

        long normalRemaining = provider.getRemainingValidity(normalToken);
        long rememberRemaining = provider.getRemainingValidity(rememberToken);

        // 记住密码的剩余有效期应明显更长（> 2天，普通模式 < 2天）
        assertTrue(rememberRemaining > 2 * 86400000L,
                "记住密码 Token 有效期应超过 2 天，实际: " + rememberRemaining + "ms");
        assertTrue(normalRemaining < 2 * 86400000L,
                "普通 Token 有效期应小于 2 天，实际: " + normalRemaining + "ms");
    }

    @Test
    @DisplayName("有效 Token 应通过验证")
    void validateToken_withValidToken_shouldReturnTrue() {
        String token = provider.generateToken(1L, "test", "1", false);
        assertTrue(provider.validateToken(token));
    }

    @Test
    @DisplayName("无效 Token 应拒绝验证")
    void validateToken_withInvalidToken_shouldReturnFalse() {
        assertFalse(provider.validateToken("invalid.jwt.token"));
        assertFalse(provider.validateToken(""));
        assertFalse(provider.validateToken("eyJ.eyJ.abc"));
    }

    @Test
    @DisplayName("过期 Token 应拒绝验证")
    void validateToken_withExpiredToken_shouldReturnFalse() throws Exception {
        // 手动创建一个已过期的 Token
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        String expiredToken = Jwts.builder()
                .claim("userId", 1L)
                .claim("username", "test")
                .claim("role", "1")
                .issuedAt(new Date(System.currentTimeMillis() - 100000))
                .expiration(new Date(System.currentTimeMillis() - 1000)) // 已过期
                .signWith(key)
                .compact();

        assertFalse(provider.validateToken(expiredToken),
                "过期 Token 应被拒绝");
    }

    @Test
    @DisplayName("不同密钥签发的 Token 应拒绝验证")
    void validateToken_withWrongKey_shouldReturnFalse() {
        // 用不同密钥手动签发
        String otherSecret = "YW5vdGhlciB0ZXN0IHNlY3JldCBrZXkgZm9yIGp3dCAyNTYgYml0cyBsb25n";
        SecretKey otherKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(otherSecret));
        String otherToken = Jwts.builder()
                .claim("userId", 1L)
                .claim("username", "test")
                .claim("role", "1")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(otherKey)
                .compact();

        assertFalse(provider.validateToken(otherToken));
    }

    @Test
    @DisplayName("解析包含记住密码参数的 Token")
    void generateToken_shouldSupportRememberMe() {
        String token = provider.generateToken(1L, "user", "1", true);
        assertNotNull(token);
        assertTrue(provider.validateToken(token));
        assertEquals(1L, provider.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("生成再解析多个 Token 应有不同的 JWT ID")
    void generateToken_shouldHaveUniqueJwtId() {
        String token1 = provider.generateToken(1L, "a", "1", false);
        String token2 = provider.generateToken(1L, "a", "1", false);
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("不同用户生成不同的 Token")
    void generateToken_differentUsers_shouldProduceDifferentTokens() {
        String token1 = provider.generateToken(1L, "alice", "1", false);
        String token2 = provider.generateToken(2L, "bob", "2", false);
        assertNotEquals(token1, token2);
    }
}

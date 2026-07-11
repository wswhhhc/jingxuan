package com.jingxuan.identityaccess.application;

import com.jingxuan.exception.UnauthorizedException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

/** Redis 中只保存 refresh token 哈希，并通过 getAndDelete 保证轮换令牌一次性消费。 */
@Service
public class RedisRefreshTokenService implements RefreshTokenService {

    private static final String PREFIX = "jingxuan:v1:refresh:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(8);
    private static final Duration REMEMBER_TTL = Duration.ofDays(30);
    private final SecureRandom secureRandom = new SecureRandom();
    private final StringRedisTemplate redis;

    public RedisRefreshTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public IssuedRefreshToken issue(Long userId, String username, String role, boolean rememberMe) {
        String token = randomToken();
        Duration ttl = rememberMe ? REMEMBER_TTL : DEFAULT_TTL;
        redis.opsForValue().set(key(token), encode(userId, username, role, rememberMe), ttl);
        return new IssuedRefreshToken(token, ttl.toSeconds());
    }

    @Override
    public RotatedRefreshToken rotate(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("refresh token 无效");
        }
        String payload = redis.opsForValue().getAndDelete(key(refreshToken));
        if (payload == null) {
            throw new UnauthorizedException("refresh token 已过期或已被使用");
        }
        Session session = decode(payload);
        IssuedRefreshToken replacement = issue(session.userId(), session.username(), session.role(), session.rememberMe());
        return new RotatedRefreshToken(session.userId(), session.username(), session.role(), replacement);
    }

    @Override
    public void revoke(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            redis.delete(key(refreshToken));
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String key(String token) {
        return PREFIX + sha256(token);
    }

    private static String encode(Long userId, String username, String role, boolean rememberMe) {
        return userId + "\t" + safe(username) + "\t" + safe(role) + "\t" + rememberMe;
    }

    private static Session decode(String payload) {
        String[] parts = payload.split("\\t", -1);
        if (parts.length != 4) {
            throw new UnauthorizedException("refresh token 会话数据损坏");
        }
        try {
            return new Session(Long.valueOf(parts[0]), parts[1], parts[2], Boolean.parseBoolean(parts[3]));
        } catch (NumberFormatException exception) {
            throw new UnauthorizedException("refresh token 会话数据损坏");
        }
    }

    private static String safe(String value) {
        if (value == null || value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("refresh token 会话字段无效");
        }
        return value;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private record Session(Long userId, String username, String role, boolean rememberMe) {
    }
}

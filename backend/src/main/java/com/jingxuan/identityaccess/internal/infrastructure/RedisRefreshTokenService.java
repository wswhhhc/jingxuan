package com.jingxuan.identityaccess.internal.infrastructure;

import com.jingxuan.exception.UnauthorizedException;
import com.jingxuan.identityaccess.application.RefreshTokenService;
import com.jingxuan.identityaccess.internal.domain.RefreshFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Redis refresh family 适配器。令牌轮换使用单个 Lua 脚本原子更新旧令牌、后继令牌与 family。
 *
 * @see <a href="https://docs.spring.io/spring-data/redis/reference/redis/scripting.html">Spring Data Redis scripting</a>
 * @see <a href="https://redis.io/docs/latest/develop/programmability/eval-intro/">Redis Lua atomic execution</a>
 * @see <a href="https://redis.io/docs/latest/commands/pexpireat/">Redis PEXPIREAT</a>
 */
@Service
public class RedisRefreshTokenService implements RefreshTokenService {

    static final String TOKEN_PREFIX = "jingxuan:v2:refresh:token:";
    static final String FAMILY_PREFIX = "jingxuan:v2:refresh:family:";
    static final String USER_PREFIX = "jingxuan:v2:refresh:user:";
    private static final int TOKEN_SECRET_BYTES = 32;
    private static final int FAMILY_ID_BYTES = 16;
    private static final int MAX_RANDOM_COLLISION_RETRIES = 3;
    private static final int INVALID = 0;
    private static final int SUCCESS = 1;
    private static final int FAMILY_REVOKED = 2;
    private static final int REVOKE_CONTEXT_STALE = 2;
    private static final int COLLISION = 3;
    private static final int CONTEXT_STALE = 4;
    private static final long MIN_ROTATION_LIFETIME_MILLIS = 1_000L;
    private static final Logger log = LoggerFactory.getLogger(RedisRefreshTokenService.class);

    private static final RedisScript<Long> ISSUE_SCRIPT = RedisScript.of("""
            local tokenKey = KEYS[1]
            local familyKey = KEYS[2]
            local userKey = KEYS[3]

            if redis.call('EXISTS', tokenKey) == 1 or redis.call('EXISTS', familyKey) == 1 then
                return 0
            end

            redis.call('HSET', tokenKey,
                'status', 'ACTIVE',
                'familyId', ARGV[1],
                'userId', ARGV[2],
                'expiresAt', ARGV[8])
            redis.call('HSET', familyKey,
                'userId', ARGV[2],
                'username', ARGV[3],
                'role', ARGV[4],
                'rememberMe', ARGV[5],
                'currentHash', ARGV[6],
                'status', 'ACTIVE',
                'expiresAt', ARGV[8])
            redis.call('PEXPIREAT', tokenKey, ARGV[8])
            redis.call('PEXPIREAT', familyKey, ARGV[8])

            redis.call('ZREMRANGEBYSCORE', userKey, '-inf', ARGV[7])
            redis.call('ZADD', userKey, ARGV[8], ARGV[1])
            local latest = redis.call('ZRANGE', userKey, -1, -1, 'WITHSCORES')
            if #latest == 2 then
                redis.call('PEXPIREAT', userKey, latest[2])
            end
            return 1
            """, Long.class);

    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> LOOKUP_SCRIPT = RedisScript.of("""
            local tokenKey = KEYS[1]
            local familyKey = KEYS[2]

            local tokenFamilyId = redis.call('HGET', tokenKey, 'familyId')
            if not tokenFamilyId or tokenFamilyId ~= ARGV[1] then
                return {0}
            end

            local tokenStatus = redis.call('HGET', tokenKey, 'status')
            local tokenUserId = redis.call('HGET', tokenKey, 'userId')
            local tokenExpiresAt = redis.call('HGET', tokenKey, 'expiresAt')
            local familyStatus = redis.call('HGET', familyKey, 'status')
            local familyUserId = redis.call('HGET', familyKey, 'userId')
            local currentHash = redis.call('HGET', familyKey, 'currentHash')
            local familyExpiresAt = redis.call('HGET', familyKey, 'expiresAt')

            if not tokenStatus or not tokenUserId or not tokenExpiresAt
                    or not familyStatus or not familyUserId or not currentHash or not familyExpiresAt then
                return {5}
            end
            if tokenUserId ~= familyUserId or tokenExpiresAt ~= familyExpiresAt then
                return {5}
            end
            return {1, tokenStatus, tokenFamilyId, tokenUserId, tokenExpiresAt,
                    familyStatus, familyUserId, currentHash, familyExpiresAt}
            """, List.class);

    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> ROTATE_SCRIPT = RedisScript.of("""
            local oldTokenKey = KEYS[1]
            local newTokenKey = KEYS[2]
            local familyKey = KEYS[3]
            local currentTokenKey = KEYS[4]
            local userKey = KEYS[5]

            local function removeFamilyFromUser()
                redis.call('ZREM', userKey, ARGV[1])
                if redis.call('ZCARD', userKey) == 0 then
                    redis.call('DEL', userKey)
                    return
                end
                local latest = redis.call('ZRANGE', userKey, -1, -1, 'WITHSCORES')
                if #latest == 2 then
                    redis.call('PEXPIREAT', userKey, latest[2])
                end
            end

            local oldFamilyId = redis.call('HGET', oldTokenKey, 'familyId')
            if not oldFamilyId or oldFamilyId ~= ARGV[1] then
                return {0}
            end

            local oldStatus = redis.call('HGET', oldTokenKey, 'status')
            local oldUserId = redis.call('HGET', oldTokenKey, 'userId')
            local tokenExpiresAtText = redis.call('HGET', oldTokenKey, 'expiresAt')
            local familyStatus = redis.call('HGET', familyKey, 'status')
            local familyUserId = redis.call('HGET', familyKey, 'userId')
            local currentHash = redis.call('HGET', familyKey, 'currentHash')
            local expiresAtText = redis.call('HGET', familyKey, 'expiresAt')

            if not oldStatus or not oldUserId or not tokenExpiresAtText or not familyStatus
                    or not familyUserId or not currentHash or not expiresAtText then
                return {5}
            end
            if currentHash ~= ARGV[5] then
                return {4}
            end
            if oldUserId ~= ARGV[4] or familyUserId ~= ARGV[4] or tokenExpiresAtText ~= expiresAtText then
                return {5}
            end

            local expiresAt = tonumber(expiresAtText)
            local now = tonumber(ARGV[6])
            if not expiresAt or not now or expiresAt - now < 1000 then
                return {0}
            end

            local function revokeFamily()
                redis.call('HSET', oldTokenKey, 'status', 'REVOKED', 'revokedAt', ARGV[6])
                redis.call('PEXPIREAT', oldTokenKey, expiresAt)
                if redis.call('HGET', currentTokenKey, 'familyId') == ARGV[1]
                        and redis.call('HGET', currentTokenKey, 'userId') == ARGV[4] then
                    redis.call('HSET', currentTokenKey, 'status', 'REVOKED', 'revokedAt', ARGV[6])
                    redis.call('PEXPIREAT', currentTokenKey, expiresAt)
                end
                redis.call('HSET', familyKey, 'status', 'REVOKED', 'revokedAt', ARGV[6])
                redis.call('PEXPIREAT', familyKey, expiresAt)
                removeFamilyFromUser()
                return {2}
            end

            if familyStatus ~= 'ACTIVE' then
                return {0}
            end
            if oldStatus == 'USED' or (oldStatus == 'ACTIVE' and currentHash ~= ARGV[2]) then
                return revokeFamily()
            end
            if oldStatus ~= 'ACTIVE' or currentHash ~= ARGV[2] then
                return {0}
            end
            if redis.call('EXISTS', newTokenKey) == 1 then
                return {3}
            end

            local username = redis.call('HGET', familyKey, 'username')
            local role = redis.call('HGET', familyKey, 'role')
            local rememberMe = redis.call('HGET', familyKey, 'rememberMe')
            if not username or not role or not rememberMe then
                return {5}
            end

            redis.call('HSET', oldTokenKey, 'status', 'USED', 'usedAt', ARGV[6])
            redis.call('HSET', newTokenKey,
                'status', 'ACTIVE',
                'familyId', ARGV[1],
                'userId', oldUserId,
                'expiresAt', expiresAtText)
            redis.call('HSET', familyKey, 'currentHash', ARGV[3])
            redis.call('PEXPIREAT', oldTokenKey, expiresAt)
            redis.call('PEXPIREAT', newTokenKey, expiresAt)
            redis.call('PEXPIREAT', familyKey, expiresAt)
            return {1, oldUserId, username, role, rememberMe, expiresAtText}
            """, List.class);

    private static final RedisScript<Long> REVOKE_SCRIPT = RedisScript.of("""
            local tokenKey = KEYS[1]
            local familyKey = KEYS[2]
            local currentTokenKey = KEYS[3]
            local userKey = KEYS[4]

            local tokenFamilyId = redis.call('HGET', tokenKey, 'familyId')
            if not tokenFamilyId or tokenFamilyId ~= ARGV[1] then
                return 0
            end

            local tokenUserId = redis.call('HGET', tokenKey, 'userId')
            local tokenExpiresAt = redis.call('HGET', tokenKey, 'expiresAt')
            local familyUserId = redis.call('HGET', familyKey, 'userId')
            local currentHash = redis.call('HGET', familyKey, 'currentHash')
            local familyExpiresAt = redis.call('HGET', familyKey, 'expiresAt')
            if not tokenUserId or not tokenExpiresAt or not familyUserId or not currentHash or not familyExpiresAt then
                return 3
            end
            if currentHash ~= ARGV[3] then
                return 2
            end
            if tokenUserId ~= ARGV[2] or familyUserId ~= ARGV[2] or tokenExpiresAt ~= familyExpiresAt then
                return 3
            end

            local expiresAt = tonumber(familyExpiresAt)
            local now = tonumber(ARGV[4])
            if not expiresAt or not now or now >= expiresAt then
                return 0
            end

            redis.call('HSET', tokenKey, 'status', 'REVOKED', 'revokedAt', ARGV[4])
            redis.call('PEXPIREAT', tokenKey, expiresAt)
            if redis.call('HGET', currentTokenKey, 'familyId') == ARGV[1]
                    and redis.call('HGET', currentTokenKey, 'userId') == ARGV[2] then
                redis.call('HSET', currentTokenKey, 'status', 'REVOKED', 'revokedAt', ARGV[4])
                redis.call('PEXPIREAT', currentTokenKey, expiresAt)
            end
            redis.call('HSET', familyKey, 'status', 'REVOKED', 'revokedAt', ARGV[4])
            redis.call('PEXPIREAT', familyKey, expiresAt)

            redis.call('ZREM', userKey, ARGV[1])
            if redis.call('ZCARD', userKey) == 0 then
                redis.call('DEL', userKey)
            else
                local latest = redis.call('ZRANGE', userKey, -1, -1, 'WITHSCORES')
                if #latest == 2 then
                    redis.call('PEXPIREAT', userKey, latest[2])
                end
            end
            return 1
            """, Long.class);

    private static final RedisScript<Long> REVOKE_ALL_SCRIPT = RedisScript.of("""
            local userKey = KEYS[1]
            local familyPrefix = ARGV[1]
            local tokenPrefix = ARGV[2]
            local userId = ARGV[3]
            local now = ARGV[4]
            local familyIds = redis.call('ZRANGE', userKey, 0, -1)
            local revoked = 0
            for _, familyId in ipairs(familyIds) do
                local familyKey = familyPrefix .. familyId
                if redis.call('HGET', familyKey, 'userId') == userId then
                    local expiresAt = tonumber(redis.call('HGET', familyKey, 'expiresAt'))
                    local currentHash = redis.call('HGET', familyKey, 'currentHash')
                    if expiresAt and currentHash and expiresAt > tonumber(now) then
                        redis.call('HSET', familyKey, 'status', 'REVOKED', 'revokedAt', now)
                        redis.call('PEXPIREAT', familyKey, expiresAt)
                        local tokenKey = tokenPrefix .. currentHash
                        if redis.call('HGET', tokenKey, 'familyId') == familyId
                                and redis.call('HGET', tokenKey, 'userId') == userId then
                            redis.call('HSET', tokenKey, 'status', 'REVOKED', 'revokedAt', now)
                            redis.call('PEXPIREAT', tokenKey, expiresAt)
                        end
                        revoked = revoked + 1
                    end
                end
            end
            redis.call('DEL', userKey)
            return revoked
            """, Long.class);

    private final StringRedisTemplate redis;
    private final Clock clock;
    private final SecureRandom secureRandom;

    @Autowired
    public RedisRefreshTokenService(StringRedisTemplate redis) {
        this(redis, Clock.systemUTC(), new SecureRandom());
    }

    RedisRefreshTokenService(StringRedisTemplate redis, Clock clock, SecureRandom secureRandom) {
        this.redis = redis;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    @Override
    public IssuedRefreshToken issue(Long userId, String username, String role, boolean rememberMe) {
        Instant now = currentInstant();
        for (int attempt = 0; attempt < MAX_RANDOM_COLLISION_RETRIES; attempt++) {
            TokenMaterial material = newFamilyToken();
            RefreshFamily family = RefreshFamily.issue(material.familyId(), userId, username, role,
                    rememberMe, material.hash(), now);
            Long result = executeRedis("issue", ISSUE_SCRIPT, List.of(
                            tokenKey(material.hash()), familyKey(material.familyId()), userKey(userId)),
                    material.familyId(), userId.toString(), username, role, rememberMe ? "1" : "0",
                    material.hash(), Long.toString(now.toEpochMilli()), Long.toString(family.expiresAt().toEpochMilli()));
            if (Long.valueOf(SUCCESS).equals(result)) {
                return new IssuedRefreshToken(material.token(), family.remainingSeconds(now));
            }
            if (!Long.valueOf(0).equals(result)) {
                throw corruptedStore();
            }
        }
        throw new IllegalStateException("无法创建 refresh token family");
    }

    @Override
    public RotatedRefreshToken rotate(String refreshToken) {
        String familyId = familyIdFromToken(refreshToken);
        String oldHash = sha256(refreshToken);

        for (int attempt = 0; attempt < MAX_RANDOM_COLLISION_RETRIES; attempt++) {
            Instant now = currentInstant();
            TokenContext context = lookupContext("rotate", familyId, oldHash);
            if (context.expiresAt().isBefore(now.plusMillis(MIN_ROTATION_LIFETIME_MILLIS))) {
                throw invalidRefreshToken();
            }
            TokenMaterial replacement = replacementToken(familyId);
            List<?> result = executeRedis("rotate", ROTATE_SCRIPT, List.of(
                            tokenKey(oldHash), tokenKey(replacement.hash()), familyKey(familyId),
                            tokenKey(context.currentHash()), userKey(context.userId())),
                    familyId, oldHash, replacement.hash(), context.userId().toString(),
                    context.currentHash(), Long.toString(now.toEpochMilli()));
            int code = resultCode(result);
            if (code == COLLISION || code == CONTEXT_STALE) {
                continue;
            }
            if (code == INVALID || code == FAMILY_REVOKED) {
                throw invalidRefreshToken();
            }
            if (code != SUCCESS) {
                throw corruptedStore();
            }
            if (result.size() != 6) {
                throw corruptedStore();
            }

            Long userId = parseLong(result.get(1));
            String username = asString(result.get(2));
            String role = asString(result.get(3));
            String rememberMeValue = asString(result.get(4));
            Instant expiresAt = Instant.ofEpochMilli(parseLong(result.get(5)));
            if (!userId.equals(context.userId()) || !expiresAt.equals(context.expiresAt())
                    || !("0".equals(rememberMeValue) || "1".equals(rememberMeValue))) {
                throw corruptedStore();
            }
            boolean rememberMe = "1".equals(rememberMeValue);
            RefreshFamily family = new RefreshFamily(familyId, userId, username, role, rememberMe,
                    replacement.hash(), RefreshFamily.Status.ACTIVE, expiresAt);
            long expiresIn = family.remainingSeconds(now);
            if (expiresIn <= 0) {
                throw invalidRefreshToken();
            }
            return new RotatedRefreshToken(userId, username, role,
                    new IssuedRefreshToken(replacement.token(), expiresIn));
        }
        throw new IllegalStateException("refresh token 状态冲突");
    }

    @Override
    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String familyId = familyIdFromToken(refreshToken);
        String hash = sha256(refreshToken);
        for (int attempt = 0; attempt < MAX_RANDOM_COLLISION_RETRIES; attempt++) {
            TokenContext context = lookupContext("revoke", familyId, hash);
            Long result = executeRedis("revoke", REVOKE_SCRIPT, List.of(
                            tokenKey(hash), familyKey(familyId), tokenKey(context.currentHash()),
                            userKey(context.userId())),
                    familyId, context.userId().toString(), context.currentHash(),
                    Long.toString(currentInstant().toEpochMilli()));
            if (Long.valueOf(SUCCESS).equals(result)) {
                return;
            }
            if (Long.valueOf(REVOKE_CONTEXT_STALE).equals(result)) {
                continue;
            }
            if (Long.valueOf(INVALID).equals(result)) {
                throw invalidRefreshToken();
            }
            throw corruptedStore();
        }
        throw new IllegalStateException("refresh token 状态冲突");
    }

    @Override
    public void revokeAll(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId 必须为正数");
        }
        executeRedis("revoke-all", REVOKE_ALL_SCRIPT, List.of(userKey(userId)), FAMILY_PREFIX, TOKEN_PREFIX,
                userId.toString(), Long.toString(currentInstant().toEpochMilli()));
    }

    private TokenContext lookupContext(String operation, String familyId, String tokenHash) {
        List<?> result = executeRedis(operation, LOOKUP_SCRIPT,
                List.of(tokenKey(tokenHash), familyKey(familyId)), familyId);
        int code = resultCode(result);
        if (code == INVALID) {
            throw invalidRefreshToken();
        }
        if (code != SUCCESS || result.size() != 9) {
            throw corruptedStore();
        }

        String tokenStatus = asString(result.get(1));
        String returnedFamilyId = asString(result.get(2));
        Long tokenUserId = parseLong(result.get(3));
        long tokenExpiresAt = parseLong(result.get(4));
        String familyStatus = asString(result.get(5));
        Long familyUserId = parseLong(result.get(6));
        String currentHash = asString(result.get(7));
        long familyExpiresAt = parseLong(result.get(8));

        if (!isTokenStatus(tokenStatus) || !isFamilyStatus(familyStatus)
                || !familyId.equals(returnedFamilyId) || !tokenUserId.equals(familyUserId)
                || familyUserId <= 0 || tokenExpiresAt != familyExpiresAt || familyExpiresAt <= 0
                || !isSha256Hash(currentHash)) {
            throw corruptedStore();
        }
        return new TokenContext(familyUserId, currentHash, Instant.ofEpochMilli(familyExpiresAt));
    }

    /** Redis 绝对过期时间以毫秒保存，所有比较使用同一精度以避免秒级 TTL 截断。 */
    private Instant currentInstant() {
        return clock.instant().truncatedTo(ChronoUnit.MILLIS);
    }

    static String familyIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            throw invalidRefreshToken();
        }
        int separator = token.indexOf('.');
        if (separator <= 0 || separator != token.lastIndexOf('.') || separator == token.length() - 1) {
            throw invalidRefreshToken();
        }
        String familyId = token.substring(0, separator);
        String secret = token.substring(separator + 1);
        try {
            byte[] decodedFamilyId = Base64.getUrlDecoder().decode(familyId);
            byte[] decodedSecret = Base64.getUrlDecoder().decode(secret);
            if (decodedFamilyId.length != FAMILY_ID_BYTES || decodedSecret.length != TOKEN_SECRET_BYTES) {
                throw invalidRefreshToken();
            }
        } catch (IllegalArgumentException exception) {
            throw invalidRefreshToken();
        }
        return familyId;
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private TokenMaterial newFamilyToken() {
        String familyId = randomPart(FAMILY_ID_BYTES);
        return replacementToken(familyId);
    }

    private TokenMaterial replacementToken(String familyId) {
        String token = familyId + "." + randomPart(TOKEN_SECRET_BYTES);
        return new TokenMaterial(familyId, token, sha256(token));
    }

    private String randomPart(int byteCount) {
        byte[] bytes = new byte[byteCount];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static int resultCode(List<?> result) {
        if (result == null || result.isEmpty()) {
            throw corruptedStore();
        }
        return Math.toIntExact(parseLong(result.get(0)));
    }

    private static long parseLong(Object value) {
        try {
            return Long.parseLong(asString(value));
        } catch (NumberFormatException exception) {
            throw corruptedStore();
        }
    }

    private static String asString(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (value == null) {
            throw corruptedStore();
        }
        return String.valueOf(value);
    }

    private <T> T executeRedis(String operation, RedisScript<T> script, List<String> keys, Object... arguments) {
        try {
            return redis.execute(script, keys, arguments);
        } catch (RuntimeException exception) {
            log.error("refresh token Redis {} 失败 ({})", operation, exception.getClass().getSimpleName());
            throw unavailableStore();
        }
    }

    private static boolean isTokenStatus(String status) {
        return "ACTIVE".equals(status) || "USED".equals(status) || "REVOKED".equals(status);
    }

    private static boolean isFamilyStatus(String status) {
        return "ACTIVE".equals(status) || "REVOKED".equals(status);
    }

    private static boolean isSha256Hash(String value) {
        return value != null && value.length() == 64
                && value.chars().allMatch(character -> character >= '0' && character <= '9'
                || character >= 'a' && character <= 'f');
    }

    private static String tokenKey(String hash) {
        return TOKEN_PREFIX + hash;
    }

    private static String familyKey(String familyId) {
        return FAMILY_PREFIX + familyId;
    }

    private static String userKey(Long userId) {
        return USER_PREFIX + userId;
    }

    private static UnauthorizedException invalidRefreshToken() {
        return new UnauthorizedException("refresh token 无效");
    }

    private static IllegalStateException unavailableStore() {
        return new IllegalStateException("refresh token 存储暂时不可用");
    }

    private static IllegalStateException corruptedStore() {
        return new IllegalStateException("refresh token 存储状态损坏");
    }

    private record TokenMaterial(String familyId, String token, String hash) {
    }

    private record TokenContext(Long userId, String currentHash, Instant expiresAt) {
    }
}

package com.jingxuan.identityaccess.internal.infrastructure;

import com.jingxuan.identityaccess.api.RateLimitService;
import com.jingxuan.identityaccess.api.RateLimitUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Redis 固定窗口限流适配器。计数与 TTL 由单个 Lua 脚本原子更新。
 */
@Service
public class RedisRateLimitService implements RateLimitService {

    static final String KEY_PREFIX = "jingxuan:v2:rate-limit:";
    private static final int MAX_LIMIT = 1_000;
    private static final int MAX_SUBJECT_LENGTH = 512;
    private static final long MAX_WINDOW_MILLIS = Duration.ofDays(1).toMillis();
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[a-z0-9][a-z0-9:-]{0,63}");
    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitService.class);

    private static final RedisScript<List> CONSUME_SCRIPT = RedisScript.of("""
            local currentValue = redis.call('GET', KEYS[1])
            local current = 0
            if currentValue then
                current = tonumber(currentValue)
                if not current or current < 0 then
                    return {-1, 0, 1}
                end
            end

            local limit = tonumber(ARGV[1])
            local windowMillis = tonumber(ARGV[2])
            local allowed = 0
            if current < limit then
                current = redis.call('INCR', KEYS[1])
                allowed = 1
            end

            local ttl = redis.call('PTTL', KEYS[1])
            if ttl == -1 then
                redis.call('PEXPIRE', KEYS[1], windowMillis)
                ttl = windowMillis
            elseif ttl == -2 then
                redis.call('SET', KEYS[1], 1, 'PX', windowMillis)
                current = 1
                allowed = 1
                ttl = windowMillis
            end
            return {allowed, current, math.max(1, math.ceil(ttl / 1000))}
            """, List.class);

    private static final RedisScript<List> INSPECT_SCRIPT = RedisScript.of("""
            local currentValue = redis.call('GET', KEYS[1])
            local windowMillis = tonumber(ARGV[2])
            if not currentValue then
                return {1, 0, math.max(1, math.ceil(windowMillis / 1000))}
            end

            local current = tonumber(currentValue)
            if not current or current < 0 then
                return {-1, 0, 1}
            end
            local ttl = redis.call('PTTL', KEYS[1])
            if ttl == -2 then
                return {1, 0, math.max(1, math.ceil(windowMillis / 1000))}
            elseif ttl == -1 then
                return {-1, current, 1}
            end
            local allowed = 0
            if current < tonumber(ARGV[1]) then
                allowed = 1
            end
            return {allowed, current, math.max(1, math.ceil(ttl / 1000))}
            """, List.class);

    private final StringRedisTemplate redis;

    public RedisRateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Decision consume(String namespace, String subject, int limit, Duration window) {
        ValidatedRequest request = validate(namespace, subject, limit, window);
        if (request == null) {
            return failClosed(limit, window);
        }
        return executeDecision("consume", CONSUME_SCRIPT, request, false);
    }

    @Override
    public Decision inspect(String namespace, String subject, int limit, Duration window) {
        ValidatedRequest request = validate(namespace, subject, limit, window);
        if (request == null) {
            return failClosed(limit, window);
        }
        return executeDecision("inspect", INSPECT_SCRIPT, request, true);
    }

    @Override
    public boolean reset(String namespace, String subject, int limit, Duration window) {
        ValidatedRequest request = validate(namespace, subject, limit, window);
        if (request == null) {
            return false;
        }
        try {
            return redis.delete(request.key()) != null;
        } catch (RuntimeException exception) {
            log.error("rate limit Redis reset 失败 ({})", exception.getClass().getSimpleName());
            throw new RateLimitUnavailableException();
        }
    }

    private Decision executeDecision(String operation, RedisScript<List> script,
                                     ValidatedRequest request, boolean inspecting) {
        final List<?> result;
        try {
            result = redis.execute(script, List.of(request.key()),
                    Integer.toString(request.limit()), Long.toString(request.windowMillis()));
        } catch (RuntimeException exception) {
            log.error("rate limit Redis {} 失败 ({})", operation, exception.getClass().getSimpleName());
            throw new RateLimitUnavailableException();
        }
        Decision decision = parseDecision(result, request, inspecting);
        if (decision == null) {
            log.error("rate limit Redis {} 返回不可信状态", operation);
            throw new RateLimitUnavailableException();
        }
        return decision;
    }

    private static Decision parseDecision(List<?> result, ValidatedRequest request, boolean inspecting) {
        if (result == null || result.size() != 3) {
            return null;
        }
        Long allowedValue = parseLong(result.get(0));
        Long count = parseLong(result.get(1));
        Long retryAfterSeconds = parseLong(result.get(2));
        long maxRetryAfterSeconds = Math.floorDiv(request.windowMillis() - 1, 1000) + 1;
        if (allowedValue == null || count == null || retryAfterSeconds == null
                || (allowedValue != 0 && allowedValue != 1)
                || count < 0 || count > request.limit()
                || retryAfterSeconds < 1 || retryAfterSeconds > maxRetryAfterSeconds) {
            return null;
        }
        boolean allowed = allowedValue == 1;
        if (inspecting) {
            if (allowed != (count < request.limit())) {
                return null;
            }
        } else if (allowed ? count < 1 || count > request.limit() : count != request.limit()) {
            return null;
        }
        return new Decision(allowed, count, retryAfterSeconds);
    }

    private static Long parseLong(Object value) {
        try {
            if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
                return ((Number) value).longValue();
            }
            if (value instanceof byte[] bytes) {
                return parseCanonicalLong(new String(bytes, StandardCharsets.UTF_8));
            }
            return value instanceof String string ? parseCanonicalLong(string) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Long parseCanonicalLong(String value) {
        if (value == null || !value.matches("0|[1-9][0-9]*")) {
            return null;
        }
        return Long.parseLong(value);
    }

    private static ValidatedRequest validate(String namespace, String subject, int limit, Duration window) {
        if (namespace == null || !NAMESPACE_PATTERN.matcher(namespace).matches()
                || subject == null || subject.isBlank() || subject.length() > MAX_SUBJECT_LENGTH
                || limit <= 0 || limit > MAX_LIMIT || window == null) {
            return null;
        }
        final long windowMillis;
        try {
            windowMillis = window.toMillis();
        } catch (ArithmeticException exception) {
            return null;
        }
        if (windowMillis <= 0 || windowMillis > MAX_WINDOW_MILLIS) {
            return null;
        }
        String policyHash = sha256(subject + '\u0000' + limit + '\u0000' + windowMillis);
        return new ValidatedRequest(KEY_PREFIX + namespace + ':' + policyHash, limit, windowMillis);
    }

    private static Decision failClosed(int limit, Duration window) {
        long retryAfterSeconds = 1;
        if (window != null && !window.isNegative() && !window.isZero()) {
            try {
                long windowMillis = window.toMillis();
                if (windowMillis > 0) {
                    retryAfterSeconds = Math.floorDiv(windowMillis - 1, 1000) + 1;
                }
            } catch (ArithmeticException ignored) {
                retryAfterSeconds = 1;
            }
        }
        long maxRetryAfterSeconds = Math.floorDiv(MAX_WINDOW_MILLIS - 1, 1000) + 1;
        return new Decision(false, Math.min(Math.max(0, limit), MAX_LIMIT),
                Math.min(retryAfterSeconds, maxRetryAfterSeconds));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM 不支持 SHA-256", exception);
        }
    }

    private record ValidatedRequest(String key, int limit, long windowMillis) {
    }
}

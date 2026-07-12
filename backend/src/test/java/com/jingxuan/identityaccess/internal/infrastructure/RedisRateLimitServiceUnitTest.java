package com.jingxuan.identityaccess.internal.infrastructure;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jingxuan.identityaccess.api.RateLimitService;
import com.jingxuan.identityaccess.api.RateLimitUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRateLimitServiceUnitTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(RedisRateLimitService.class);
    private ListAppender<ILoggingEvent> appender;

    @AfterEach
    void detachAppender() {
        if (appender != null) {
            logger.detachAppender(appender);
        }
    }

    @Test
    void consumeMapsAtomicFixedWindowDecisionAndReusesOneScript() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        List<ScriptCall> calls = new ArrayList<>();
        Deque<List<Long>> results = new ArrayDeque<>(List.of(
                List.of(1L, 1L, 60L),
                List.of(1L, 20L, 30L),
                List.of(0L, 20L, 29L)
        ));
        doAnswer(invocation -> {
            calls.add(new ScriptCall(invocation.getArgument(0), invocation.getArgument(1)));
            return results.removeFirst();
        }).when(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
        RedisRateLimitService service = new RedisRateLimitService(redis);

        RateLimitService.Decision first = service.consume(
                "public-api", "203.0.113.10:/public/works", 20, Duration.ofMinutes(1));
        RateLimitService.Decision twentieth = service.consume(
                "public-api", "203.0.113.10:/public/works", 20, Duration.ofMinutes(1));
        RateLimitService.Decision rejected = service.consume(
                "public-api", "203.0.113.10:/public/works", 20, Duration.ofMinutes(1));

        assertEquals(new RateLimitService.Decision(true, 1, 60), first);
        assertEquals(new RateLimitService.Decision(true, 20, 30), twentieth);
        assertEquals(new RateLimitService.Decision(false, 20, 29), rejected);
        assertSame(calls.get(0).script(), calls.get(1).script());
        assertSame(calls.get(1).script(), calls.get(2).script());
        assertTrue(calls.get(0).script().getScriptAsString().contains("INCR"));
        assertTrue(calls.get(0).script().getScriptAsString().contains("PEXPIRE"));
        assertTrue(calls.get(0).script().getScriptAsString().contains("ttl == -1"));
        assertFalse(calls.get(0).script().getScriptAsString().contains("ttl < 1"));
    }

    @Test
    void keyHashesSubjectAndSeparatesDifferentPolicies() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        List<List<String>> keys = new ArrayList<>();
        doAnswer(invocation -> {
            keys.add(List.copyOf(invocation.getArgument(1)));
            return List.of(1L, 1L, 60L);
        }).when(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
        RedisRateLimitService service = new RedisRateLimitService(redis);

        service.consume("login-ip", "ip:203.0.113.10", 5, Duration.ofMinutes(15));
        service.consume("login-ip", "ip:203.0.113.10", 20, Duration.ofMinutes(15));
        service.consume("login-ip", "ip:203.0.113.10", 20, Duration.ofHours(1));

        assertFalse(keys.stream().flatMap(List::stream).anyMatch(key -> key.contains("203.0.113.10")));
        assertNotEquals(keys.get(0), keys.get(1));
        assertNotEquals(keys.get(1), keys.get(2));
    }

    @Test
    void invalidParametersAreDeniedWithoutCallingRedis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisRateLimitService service = new RedisRateLimitService(redis);

        RateLimitService.Decision invalidNamespace = service.inspect(
                "Public API", "ip:203.0.113.10", 20, Duration.ofSeconds(1));
        RateLimitService.Decision invalidWindow = service.inspect(
                "public-api", "ip:203.0.113.10", 20, Duration.ofNanos(1));
        RateLimitService.Decision excessiveLimit = service.inspect(
                "public-api", "ip:203.0.113.10", 1_001, Duration.ofMinutes(1));
        RateLimitService.Decision excessiveWindow = service.inspect(
                "public-api", "ip:203.0.113.10", 20, Duration.ofDays(2));
        RateLimitService.Decision excessiveSubject = service.inspect(
                "public-api", "x".repeat(513), 20, Duration.ofMinutes(1));

        assertFalse(invalidNamespace.allowed());
        assertFalse(invalidWindow.allowed());
        assertFalse(excessiveLimit.allowed());
        assertFalse(excessiveWindow.allowed());
        assertFalse(excessiveSubject.allowed());
        assertTrue(invalidNamespace.retryAfterSeconds() >= 1);
        assertTrue(invalidWindow.retryAfterSeconds() >= 1);
        verify(redis, never()).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    @Test
    void inspectAllowsOnlyCountsStrictlyBelowLimit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        List<List<Long>> results = new ArrayList<>(List.of(
                List.of(1L, 19L, 7L),
                List.of(0L, 20L, 6L)
        ));
        doAnswer(invocation -> results.remove(0))
                .when(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
        RedisRateLimitService service = new RedisRateLimitService(redis);

        RateLimitService.Decision available = service.inspect(
                "login-ip", "ip:203.0.113.10", 20, Duration.ofMinutes(15));
        RateLimitService.Decision exhausted = service.inspect(
                "login-ip", "ip:203.0.113.10", 20, Duration.ofMinutes(15));

        assertEquals(new RateLimitService.Decision(true, 19, 7), available);
        assertEquals(new RateLimitService.Decision(false, 20, 6), exhausted);
    }

    @Test
    void contradictoryOrNonIntegralRedisDecisionsFailClosed() {
        assertInvalidDecision(List.of(1L, 0L, 60L), false);
        assertInvalidDecision(List.of(0L, 19L, 60L), false);
        assertInvalidDecision(List.of(1L, 1L, 61L), false);
        assertInvalidDecision(List.of(1L, 20L, 60L), true);
        assertInvalidDecision(List.of(1.5D, 1L, 60L), false);
    }

    @Test
    void resetIsIdempotentButInvalidParametersDoNotTouchRedis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.delete(anyString())).thenReturn(false);
        RedisRateLimitService service = new RedisRateLimitService(redis);

        assertTrue(service.reset("login-account", "account:student", 5, Duration.ofMinutes(15)));
        assertFalse(service.reset("Login Account", "account:student", 5, Duration.ofMinutes(15)));
        verify(redis).delete(anyString());
    }

    @Test
    void redisFailureThrowsSanitizedUnavailableAndLogsNoSubjectOrKey() {
        String subject = "ip:203.0.113.10";
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        List<String> redisKeys = new ArrayList<>();
        doAnswer(invocation -> {
            List<String> keys = invocation.getArgument(1);
            redisKeys.addAll(keys);
            throw new DataAccessResourceFailureException(
                    "subject=" + subject + " key=" + keys.get(0));
        }).when(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
        RedisRateLimitService service = new RedisRateLimitService(redis);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        RateLimitUnavailableException exception = assertThrows(RateLimitUnavailableException.class,
                () -> service.inspect("public-api", subject, 20, Duration.ofSeconds(1)));

        assertEquals("限流服务暂时不可用，请稍后重试", exception.getMessage());
        String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + '\n' + right);
        assertTrue(logs.contains("Redis inspect 失败"));
        assertFalse(logs.contains(subject));
        assertFalse(redisKeys.isEmpty());
        assertFalse(logs.contains(redisKeys.get(0)));
    }

    private record ScriptCall(RedisScript<?> script, List<String> keys) {
    }

    private static void assertInvalidDecision(List<?> redisResult, boolean inspect) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        doAnswer(invocation -> redisResult)
                .when(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
        RedisRateLimitService service = new RedisRateLimitService(redis);

        assertThrows(RateLimitUnavailableException.class, () -> {
            if (inspect) {
                service.inspect("public-api", "ip:203.0.113.10", 20, Duration.ofMinutes(1));
            } else {
                service.consume("public-api", "ip:203.0.113.10", 20, Duration.ofMinutes(1));
            }
        });
    }
}

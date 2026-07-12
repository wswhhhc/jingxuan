package com.jingxuan.identityaccess.internal.infrastructure;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jingxuan.identityaccess.api.ChallengePurpose;
import com.jingxuan.identityaccess.api.ChallengeService;
import com.jingxuan.identityaccess.api.ChallengeUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedisChallengeServiceTest {

    private static final String VALID_ID = "AAAAAAAAAAAAAAAAAAAAAA";

    private final Logger logger = (Logger) LoggerFactory.getLogger(RedisChallengeService.class);
    private ListAppender<ILoggingEvent> appender;

    @AfterEach
    void detachAppender() {
        if (appender != null) {
            logger.detachAppender(appender);
        }
    }

    @Test
    void issueStoresOnlyPurposeAndAnswerForExactlyFiveMinutes() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        RedisChallengeService service = service(redis);

        ChallengeService.IssuedChallenge issued = service.issue(ChallengePurpose.LOGIN);

        assertTrue(issued.challengeId().matches("[A-Za-z0-9_-]{22}"));
        assertEquals(16, Base64.getUrlDecoder().decode(issued.challengeId()).length);
        assertEquals("1 + 1 = ?", issued.question());
        assertEquals(300, issued.expiresIn());
        verify(values).set(eq(RedisChallengeService.KEY_PREFIX + issued.challengeId()),
                eq("LOGIN:2"), eq(Duration.ofSeconds(300)));
    }

    @Test
    void correctAnswerSucceedsOnlyOnceThroughAnAtomicGetAndDeleteScript() {
        ScriptHarness harness = harness("LOGIN:7", null);

        assertTrue(harness.service().verifyAndConsume(VALID_ID, ChallengePurpose.LOGIN, 7));
        assertFalse(harness.service().verifyAndConsume(VALID_ID, ChallengePurpose.LOGIN, 7));

        assertEquals(2, harness.calls().size());
        ScriptCall call = harness.calls().get(0);
        assertEquals(List.of(RedisChallengeService.KEY_PREFIX + VALID_ID), call.keys());
        assertTrue(call.script().getScriptAsString().contains("GET"));
        assertTrue(call.script().getScriptAsString().contains("DEL"));
    }

    @Test
    void wrongAnswerStillConsumesTheChallenge() {
        ScriptHarness harness = harness("LOGIN:7", null);

        assertFalse(harness.service().verifyAndConsume(VALID_ID, ChallengePurpose.LOGIN, 8));
        assertFalse(harness.service().verifyAndConsume(VALID_ID, ChallengePurpose.LOGIN, 7));

        assertEquals(2, harness.calls().size());
    }

    @Test
    void wrongPurposeStillConsumesTheChallenge() {
        ScriptHarness harness = harness("LOGIN:7", null);

        assertFalse(harness.service().verifyAndConsume(VALID_ID, ChallengePurpose.GUEST_COMMENT, 7));
        assertFalse(harness.service().verifyAndConsume(VALID_ID, ChallengePurpose.LOGIN, 7));

        assertEquals(2, harness.calls().size());
    }

    @Test
    void illegalInputsFailWithoutTouchingRedis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisChallengeService service = service(redis);

        assertFalse(service.verifyAndConsume(null, ChallengePurpose.LOGIN, 1));
        assertFalse(service.verifyAndConsume(" ", ChallengePurpose.LOGIN, 1));
        assertFalse(service.verifyAndConsume("not-url-safe!", ChallengePurpose.LOGIN, 1));
        assertFalse(service.verifyAndConsume(VALID_ID, null, 1));
        assertFalse(service.verifyAndConsume(VALID_ID, ChallengePurpose.LOGIN, null));
        assertThrows(IllegalArgumentException.class, () -> service.issue(null));
        verifyNoInteractions(redis);
    }

    @Test
    void redisFailureFailsClosedAndLogsNoChallengeMaterial() {
        String key = RedisChallengeService.KEY_PREFIX + VALID_ID;
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        doAnswer(invocation -> {
            throw new DataAccessResourceFailureException(
                    "Redis unavailable for id=" + VALID_ID + ", key=" + key + ", answer=7");
        }).when(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
        RedisChallengeService service = service(redis);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        ChallengeUnavailableException failure = assertThrows(ChallengeUnavailableException.class,
                () -> service.verifyAndConsume(VALID_ID, ChallengePurpose.LOGIN, 7));

        assertEquals("登录安全校验服务暂时不可用，请稍后重试", failure.getMessage());
        assertEquals(503, failure.status());
        assertEquals("CHALLENGE_UNAVAILABLE", failure.problemCode());
        assertEquals(1, failure.retryAfterSeconds());
        assertNull(failure.getCause());
        String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
        assertTrue(logs.contains("Redis consume 失败"));
        assertFalse(logs.contains(VALID_ID));
        assertFalse(logs.contains(key));
        assertFalse(logs.contains("answer=7"));
    }

    @Test
    void redisFailureDuringIssueAlsoFailsClosedWithoutLoggingIdKeyOrAnswer() {
        String key = RedisChallengeService.KEY_PREFIX + VALID_ID;
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        doAnswer(invocation -> {
            throw new DataAccessResourceFailureException(
                    "Redis unavailable for id=" + VALID_ID + ", key=" + key + ", value=LOGIN:2");
        }).when(values).set(anyString(), anyString(), any(Duration.class));
        RedisChallengeService service = service(redis);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        ChallengeUnavailableException failure = assertThrows(ChallengeUnavailableException.class,
                () -> service.issue(ChallengePurpose.LOGIN));

        assertEquals("登录安全校验服务暂时不可用，请稍后重试", failure.getMessage());
        assertEquals(503, failure.status());
        assertEquals("CHALLENGE_UNAVAILABLE", failure.problemCode());
        assertEquals(1, failure.retryAfterSeconds());
        assertNull(failure.getCause());
        String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
        assertTrue(logs.contains("Redis issue 失败"));
        assertFalse(logs.contains(VALID_ID));
        assertFalse(logs.contains(key));
        assertFalse(logs.contains("LOGIN:2"));
    }

    private static RedisChallengeService service(StringRedisTemplate redis) {
        return new RedisChallengeService(redis, new FixedSecureRandom());
    }

    private static ScriptHarness harness(Object... responses) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        List<Object> queued = new ArrayList<>(Arrays.asList(responses));
        AtomicInteger nextResponse = new AtomicInteger();
        List<ScriptCall> calls = new ArrayList<>();
        doAnswer(invocation -> {
            RedisScript<?> script = invocation.getArgument(0);
            List<String> keys = invocation.getArgument(1);
            calls.add(new ScriptCall(script, List.copyOf(keys)));
            return queued.get(nextResponse.getAndIncrement());
        }).when(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
        return new ScriptHarness(service(redis), calls);
    }

    private record ScriptHarness(RedisChallengeService service, List<ScriptCall> calls) {
    }

    private record ScriptCall(RedisScript<?> script, List<String> keys) {
    }

    private static final class FixedSecureRandom extends SecureRandom {

        @Override
        public void nextBytes(byte[] bytes) {
            Arrays.fill(bytes, (byte) 0);
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}

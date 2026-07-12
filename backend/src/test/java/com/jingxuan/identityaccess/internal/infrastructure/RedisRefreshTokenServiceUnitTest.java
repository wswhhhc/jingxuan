package com.jingxuan.identityaccess.internal.infrastructure;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jingxuan.exception.UnauthorizedException;
import com.jingxuan.identityaccess.application.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class RedisRefreshTokenServiceUnitTest {

    private static final Instant NOW = Instant.parse("2026-07-11T10:00:00Z");
    private static final Instant EXPIRES_AT = NOW.plus(Duration.ofHours(8));
    private static final String FAMILY_ID = "AQEBAQEBAQEBAQEBAQEBAQ";
    private static final String TOKEN = FAMILY_ID + "." + "AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgI";
    private static final String TOKEN_HASH = RedisRefreshTokenService.sha256(TOKEN);

    private final Logger logger = (Logger) LoggerFactory.getLogger(RedisRefreshTokenService.class);
    private ListAppender<ILoggingEvent> appender;

    @AfterEach
    void detachAppender() {
        if (appender != null) {
            logger.detachAppender(appender);
        }
    }

    @Test
    void unknownTokenWithRealFamilyIdStopsAtReadOnlyLookupDuringRotation() {
        ScriptHarness harness = harness(List.of(0L));

        assertThrows(UnauthorizedException.class, () -> harness.service().rotate(TOKEN));

        assertEquals(1, harness.calls().size());
        assertReadOnlyLookup(harness.calls().get(0));
    }

    @Test
    void unknownTokenWithRealFamilyIdStopsAtReadOnlyLookupDuringRevoke() {
        ScriptHarness harness = harness(List.of(0L));

        assertThrows(UnauthorizedException.class, () -> harness.service().revoke(TOKEN));

        assertEquals(1, harness.calls().size());
        assertReadOnlyLookup(harness.calls().get(0));
    }

    @Test
    void rotationUsesExplicitOldNewFamilyCurrentAndUserKeys() {
        ScriptHarness harness = harness(
                activeLookup(TOKEN_HASH, EXPIRES_AT),
                List.of(1L, "7", "student", "STUDENT", "0", Long.toString(EXPIRES_AT.toEpochMilli())));

        RefreshTokenService.RotatedRefreshToken rotated = harness.service().rotate(TOKEN);

        assertEquals(2, harness.calls().size());
        assertReadOnlyLookup(harness.calls().get(0));
        ScriptCall mutation = harness.calls().get(1);
        String replacementHash = RedisRefreshTokenService.sha256(rotated.replacement().token());
        assertEquals(List.of(
                RedisRefreshTokenService.TOKEN_PREFIX + TOKEN_HASH,
                RedisRefreshTokenService.TOKEN_PREFIX + replacementHash,
                RedisRefreshTokenService.FAMILY_PREFIX + FAMILY_ID,
                RedisRefreshTokenService.TOKEN_PREFIX + TOKEN_HASH,
                RedisRefreshTokenService.USER_PREFIX + "7"), mutation.keys());
        String script = mutation.script().getScriptAsString();
        assertTrue(script.contains("KEYS[5]"));
        assertTrue(script.indexOf("expiresAt - now < 1000") < script.indexOf("local function revokeFamily"));
        assertEquals(Duration.ofHours(8).toSeconds(), rotated.replacement().expiresIn());
    }

    @Test
    void revokeUsesExplicitTokenFamilyCurrentAndUserKeys() {
        ScriptHarness harness = harness(activeLookup(TOKEN_HASH, EXPIRES_AT), 1L);

        harness.service().revoke(TOKEN);

        assertEquals(2, harness.calls().size());
        ScriptCall mutation = harness.calls().get(1);
        assertEquals(List.of(
                RedisRefreshTokenService.TOKEN_PREFIX + TOKEN_HASH,
                RedisRefreshTokenService.FAMILY_PREFIX + FAMILY_ID,
                RedisRefreshTokenService.TOKEN_PREFIX + TOKEN_HASH,
                RedisRefreshTokenService.USER_PREFIX + "7"), mutation.keys());
        assertTrue(mutation.script().getScriptAsString().contains("ZREM"));
    }

    @Test
    void revokeAllUsesOnlyTheUserSessionIndexAndMarksAllFamiliesRevoked() {
        ScriptHarness harness = harness(2L);

        harness.service().revokeAll(7L);

        assertEquals(1, harness.calls().size());
        ScriptCall mutation = harness.calls().get(0);
        assertEquals(List.of(RedisRefreshTokenService.USER_PREFIX + "7"), mutation.keys());
        String script = mutation.script().getScriptAsString();
        assertTrue(script.contains("ZRANGE"));
        assertTrue(script.contains("status', 'REVOKED'"));
        assertTrue(script.contains("DEL', userKey"));
    }

    @Test
    void replayResultMapsToUnauthorizedInsteadOfServerFailure() {
        ScriptHarness harness = harness(activeLookup(TOKEN_HASH, EXPIRES_AT), List.of(2L));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> harness.service().rotate(TOKEN));

        assertInstanceOf(UnauthorizedException.class, exception);
    }

    @Test
    void staleContextIsReloadedBeforeTheReplayMutation() {
        String replacementHash = "b".repeat(64);
        ScriptHarness harness = harness(
                activeLookup(TOKEN_HASH, EXPIRES_AT),
                List.of(4L),
                activeLookup(replacementHash, EXPIRES_AT),
                List.of(2L));

        assertThrows(UnauthorizedException.class, () -> harness.service().rotate(TOKEN));

        assertEquals(4, harness.calls().size());
        assertEquals(RedisRefreshTokenService.TOKEN_PREFIX + replacementHash,
                harness.calls().get(3).keys().get(3));
    }

    @Test
    void lessThanOneSecondRemainingIsRejectedBeforeAnyMutatingScript() {
        Instant almostExpired = NOW.plusMillis(999);
        ScriptHarness harness = harness(activeLookup(TOKEN_HASH, almostExpired));

        assertThrows(UnauthorizedException.class, () -> harness.service().rotate(TOKEN));

        assertEquals(1, harness.calls().size());
        assertReadOnlyLookup(harness.calls().get(0));
    }

    @Test
    void redisFailureIsA500ClassFailureAndLogsNoTokenMaterialOrRedisKey() {
        String tokenKey = RedisRefreshTokenService.TOKEN_PREFIX + TOKEN_HASH;
        String familyKey = RedisRefreshTokenService.FAMILY_PREFIX + FAMILY_ID;
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        doAnswer(invocation -> {
            throw new DataAccessResourceFailureException(
                    "Redis unavailable for " + TOKEN + " " + TOKEN_HASH + " " + tokenKey + " " + familyKey);
        }).when(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
        RedisRefreshTokenService service = service(redis);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        RuntimeException failure = assertThrows(RuntimeException.class, () -> service.rotate(TOKEN));

        assertFalse(failure instanceof UnauthorizedException);
        assertEquals("refresh token 存储暂时不可用", failure.getMessage());
        assertNull(failure.getCause());
        String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
        assertTrue(logs.contains("Redis rotate 失败"));
        assertFalse(logs.contains(TOKEN));
        assertFalse(logs.contains(TOKEN_HASH));
        assertFalse(logs.contains(tokenKey));
        assertFalse(logs.contains(familyKey));
    }

    @Test
    void redisFailuresDuringIssueAndRevokeAlsoFailClosed() {
        for (String operation : List.of("issue", "revoke")) {
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            doAnswer(invocation -> {
                throw new DataAccessResourceFailureException("sensitive Redis diagnostic");
            }).when(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
            RedisRefreshTokenService service = service(redis);

            RuntimeException failure = assertThrows(RuntimeException.class, () -> {
                if (operation.equals("issue")) {
                    service.issue(7L, "student", "STUDENT", false);
                } else {
                    service.revoke(TOKEN);
                }
            });

            assertFalse(failure instanceof UnauthorizedException);
            assertEquals("refresh token 存储暂时不可用", failure.getMessage());
            assertNull(failure.getCause());
        }
    }

    @Test
    void corruptScriptResultMapsToSanitizedServerFailure() {
        ScriptHarness harness = harness(List.of(5L));

        RuntimeException failure = assertThrows(RuntimeException.class, () -> harness.service().rotate(TOKEN));

        assertFalse(failure instanceof UnauthorizedException);
        assertEquals("refresh token 存储状态损坏", failure.getMessage());
        assertNull(failure.getCause());
    }

    @Test
    void mismatchedRotationSuccessPayloadMapsToServerFailure() {
        ScriptHarness harness = harness(
                activeLookup(TOKEN_HASH, EXPIRES_AT),
                List.of(1L, "8", "student", "STUDENT", "unexpected",
                        Long.toString(EXPIRES_AT.plusSeconds(1).toEpochMilli())));

        RuntimeException failure = assertThrows(RuntimeException.class, () -> harness.service().rotate(TOKEN));

        assertFalse(failure instanceof UnauthorizedException);
        assertEquals("refresh token 存储状态损坏", failure.getMessage());
    }

    @Test
    void sha256IsAlwaysLowercase64CharacterHex() {
        String hash = RedisRefreshTokenService.sha256("refresh-token");

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }

    private static List<?> activeLookup(String currentHash, Instant expiresAt) {
        return List.of(1L, "ACTIVE", FAMILY_ID, "7", Long.toString(expiresAt.toEpochMilli()),
                "ACTIVE", "7", currentHash, Long.toString(expiresAt.toEpochMilli()));
    }

    private static ScriptHarness harness(Object... responses) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        List<ScriptCall> calls = new ArrayList<>();
        Deque<Object> queued = new ArrayDeque<>(Arrays.asList(responses));
        doAnswer(invocation -> {
            RedisScript<?> script = invocation.getArgument(0);
            List<String> keys = invocation.getArgument(1);
            Object[] invocationArguments = invocation.getArguments();
            Object[] arguments = Arrays.copyOfRange(invocationArguments, 2, invocationArguments.length);
            calls.add(new ScriptCall(script, List.copyOf(keys), arguments));
            if (queued.isEmpty()) {
                throw new AssertionError("unexpected Redis script execution");
            }
            return queued.removeFirst();
        }).when(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
        return new ScriptHarness(service(redis), calls);
    }

    private static RedisRefreshTokenService service(StringRedisTemplate redis) {
        return new RedisRefreshTokenService(redis, Clock.fixed(NOW, ZoneOffset.UTC), new FixedSecureRandom());
    }

    private static void assertReadOnlyLookup(ScriptCall call) {
        assertEquals(List.of(
                RedisRefreshTokenService.TOKEN_PREFIX + TOKEN_HASH,
                RedisRefreshTokenService.FAMILY_PREFIX + FAMILY_ID), call.keys());
        String script = call.script().getScriptAsString();
        assertFalse(script.contains("HSET"));
        assertFalse(script.contains("DEL"));
        assertFalse(script.contains("PEXPIRE"));
        assertFalse(script.contains("ZADD"));
        assertFalse(script.contains("ZREM"));
    }

    private record ScriptHarness(RedisRefreshTokenService service, List<ScriptCall> calls) {
    }

    private record ScriptCall(RedisScript<?> script, List<String> keys, Object[] arguments) {
    }

    private static final class FixedSecureRandom extends SecureRandom {

        private byte next = 3;

        @Override
        public void nextBytes(byte[] bytes) {
            Arrays.fill(bytes, next++);
        }
    }
}

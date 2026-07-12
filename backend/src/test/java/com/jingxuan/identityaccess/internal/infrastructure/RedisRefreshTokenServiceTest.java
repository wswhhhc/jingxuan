package com.jingxuan.identityaccess.internal.infrastructure;

import com.jingxuan.TestContainerImages;
import com.jingxuan.exception.UnauthorizedException;
import com.jingxuan.identityaccess.application.RefreshTokenService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@Testcontainers
class RedisRefreshTokenServiceTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(TestContainerImages.redis())
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;

    @BeforeAll
    static void connect() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
                REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void flushRedis() {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @Test
    void concurrentRotationHasOneSuccessAndRevokesItsReplacement() throws Exception {
        RedisRefreshTokenService service = service(Clock.systemUTC());
        RefreshTokenService.IssuedRefreshToken issued = service.issue(7L, "student", "STUDENT", false);
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<RotationOutcome> rotation = () -> {
                start.await();
                try {
                    return RotationOutcome.success(service.rotate(issued.token()).replacement().token());
                } catch (UnauthorizedException exception) {
                    return RotationOutcome.rejected();
                }
            };

            Future<RotationOutcome> first = executor.submit(rotation);
            Future<RotationOutcome> second = executor.submit(rotation);
            List<RotationOutcome> outcomes = List.of(first.get(), second.get());
            List<RotationOutcome> successes = outcomes.stream().filter(RotationOutcome::success).toList();

            assertEquals(1, successes.size());
            assertEquals(1, outcomes.stream().filter(outcome -> !outcome.success()).count());
            assertThrows(UnauthorizedException.class,
                    () -> service.rotate(successes.getFirst().replacementToken()));
            assertEquals("REVOKED", redis.opsForHash().get(
                    tokenKey(successes.getFirst().replacementToken()), "status"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void replayingUsedTokenRevokesTheWholeFamily() {
        RedisRefreshTokenService service = service(Clock.systemUTC());
        RefreshTokenService.IssuedRefreshToken issued = service.issue(7L, "student", "STUDENT", false);
        String replacement = service.rotate(issued.token()).replacement().token();

        assertThrows(UnauthorizedException.class, () -> service.rotate(issued.token()));
        assertThrows(UnauthorizedException.class, () -> service.rotate(replacement));
        String familyId = RedisRefreshTokenService.familyIdFromToken(issued.token());
        assertEquals("REVOKED", redis.opsForHash().get(tokenKey(replacement), "status"));
        assertEquals("REVOKED", redis.opsForHash().get(familyKey(familyId), "status"));
        assertNull(redis.opsForZSet().score(userKey(7L), familyId));
    }

    @Test
    void rotationReportsOnlyTheRemainingAbsoluteLifetime() {
        MutableClock clock = new MutableClock(Instant.now(), ZoneOffset.UTC);
        RedisRefreshTokenService service = service(clock);
        RefreshTokenService.IssuedRefreshToken issued = service.issue(7L, "student", "STUDENT", false);
        String familyId = RedisRefreshTokenService.familyIdFromToken(issued.token());
        String familyKey = familyKey(familyId);
        String oldTokenKey = tokenKey(issued.token());
        String userKey = userKey(7L);
        Object expiresAtBefore = redis.opsForHash().get(familyKey, "expiresAt");
        long absoluteDeadline = Long.parseLong(String.valueOf(expiresAtBefore));
        long tokenTtlBefore = assertTtlWithinAbsoluteDeadline(oldTokenKey, absoluteDeadline);
        long familyTtlBefore = assertTtlWithinAbsoluteDeadline(familyKey, absoluteDeadline);
        long userTtlBefore = assertTtlWithinAbsoluteDeadline(userKey, absoluteDeadline);

        clock.advance(Duration.ofHours(2));
        RefreshTokenService.RotatedRefreshToken rotated = service.rotate(issued.token());

        assertEquals(Duration.ofHours(6).toSeconds(), rotated.replacement().expiresIn());
        assertEquals(expiresAtBefore, redis.opsForHash().get(familyKey, "expiresAt"));
        long oldTokenTtlAfter = assertTtlWithinAbsoluteDeadline(oldTokenKey, absoluteDeadline);
        long replacementTtl = assertTtlWithinAbsoluteDeadline(tokenKey(rotated.replacement().token()), absoluteDeadline);
        long familyTtlAfter = assertTtlWithinAbsoluteDeadline(familyKey, absoluteDeadline);
        long userTtlAfter = assertTtlWithinAbsoluteDeadline(userKey, absoluteDeadline);
        assertTrue(oldTokenTtlAfter <= tokenTtlBefore);
        assertTrue(replacementTtl <= tokenTtlBefore);
        assertTrue(familyTtlAfter <= familyTtlBefore);
        assertTrue(userTtlAfter <= userTtlBefore);
    }

    @Test
    void redisNeverStoresThePlainRefreshToken() {
        RedisRefreshTokenService service = service(Clock.systemUTC());
        RefreshTokenService.IssuedRefreshToken issued = service.issue(7L, "student", "STUDENT", true);
        String familyId = RedisRefreshTokenService.familyIdFromToken(issued.token());
        String tokenKey = tokenKey(issued.token());
        String familyKey = familyKey(familyId);

        assertTrue(Boolean.TRUE.equals(redis.hasKey(tokenKey)));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(RedisRefreshTokenService.TOKEN_PREFIX + issued.token())));
        assertTrue(redis.opsForHash().entries(familyKey).values().stream()
                .map(String::valueOf)
                .noneMatch(value -> value.contains(issued.token())));
    }

    @Test
    void revokeInvalidatesTheFamily() {
        RedisRefreshTokenService service = service(Clock.systemUTC());
        RefreshTokenService.IssuedRefreshToken issued = service.issue(7L, "student", "STUDENT", false);

        service.revoke(issued.token());

        assertThrows(UnauthorizedException.class, () -> service.rotate(issued.token()));
        String familyId = RedisRefreshTokenService.familyIdFromToken(issued.token());
        assertEquals("REVOKED", redis.opsForHash().get(familyKey(familyId), "status"));
        assertEquals("REVOKED", redis.opsForHash().get(tokenKey(issued.token()), "status"));
        assertNull(redis.opsForZSet().score(userKey(7L), familyId));
    }

    @Test
    void forgedTokenWithARealFamilyIdCannotRevokeThatFamily() {
        RedisRefreshTokenService service = service(Clock.systemUTC());
        RefreshTokenService.IssuedRefreshToken issued = service.issue(7L, "student", "STUDENT", false);
        String familyId = RedisRefreshTokenService.familyIdFromToken(issued.token());
        String forgedSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
        String forged = familyId + "." + forgedSecret;

        assertThrows(UnauthorizedException.class, () -> service.rotate(forged));
        assertThrows(UnauthorizedException.class, () -> service.revoke(forged));

        assertEquals("ACTIVE", redis.opsForHash().get(familyKey(familyId), "status"));
        assertNotNull(service.rotate(issued.token()).replacement().token());
    }

    @Test
    void nonCurrentActiveTokenRevokesTheFamilyAndCurrentReplacement() {
        RedisRefreshTokenService service = service(Clock.systemUTC());
        RefreshTokenService.IssuedRefreshToken issued = service.issue(7L, "student", "STUDENT", false);
        String replacement = service.rotate(issued.token()).replacement().token();
        String familyId = RedisRefreshTokenService.familyIdFromToken(issued.token());
        redis.opsForHash().put(tokenKey(issued.token()), "status", "ACTIVE");

        assertThrows(UnauthorizedException.class, () -> service.rotate(issued.token()));

        assertEquals("REVOKED", redis.opsForHash().get(familyKey(familyId), "status"));
        assertEquals("REVOKED", redis.opsForHash().get(tokenKey(replacement), "status"));
        assertNull(redis.opsForZSet().score(userKey(7L), familyId));
    }

    @Test
    void lessThanOneSecondOfAbsoluteLifetimeCannotWriteAReplacement() {
        MutableClock clock = new MutableClock(Instant.now(), ZoneOffset.UTC);
        RedisRefreshTokenService service = service(clock);
        RefreshTokenService.IssuedRefreshToken issued = service.issue(7L, "student", "STUDENT", false);
        String familyId = RedisRefreshTokenService.familyIdFromToken(issued.token());
        String familyKey = familyKey(familyId);
        String tokenKey = tokenKey(issued.token());
        Map<Object, Object> familyBefore = Map.copyOf(redis.opsForHash().entries(familyKey));
        Map<Object, Object> tokenBefore = Map.copyOf(redis.opsForHash().entries(tokenKey));
        Set<String> tokenKeysBefore = Set.copyOf(redis.keys(RedisRefreshTokenService.TOKEN_PREFIX + "*"));
        clock.advance(Duration.ofHours(8).minusMillis(500));

        assertThrows(UnauthorizedException.class, () -> service.rotate(issued.token()));

        assertEquals(familyBefore, redis.opsForHash().entries(familyKey));
        assertEquals(tokenBefore, redis.opsForHash().entries(tokenKey));
        assertEquals(tokenKeysBefore, redis.keys(RedisRefreshTokenService.TOKEN_PREFIX + "*"));
    }

    private static RedisRefreshTokenService service(Clock clock) {
        return new RedisRefreshTokenService(redis, clock, new SecureRandom());
    }

    private static String tokenKey(String token) {
        return RedisRefreshTokenService.TOKEN_PREFIX + RedisRefreshTokenService.sha256(token);
    }

    private static String familyKey(String familyId) {
        return RedisRefreshTokenService.FAMILY_PREFIX + familyId;
    }

    private static String userKey(Long userId) {
        return RedisRefreshTokenService.USER_PREFIX + userId;
    }

    private static long assertTtlWithinAbsoluteDeadline(String key, long absoluteDeadline) {
        Long ttl = redis.getExpire(key, TimeUnit.MILLISECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0, () -> key + " should retain a positive PTTL");
        long remainingUntilDeadline = absoluteDeadline - System.currentTimeMillis();
        assertTrue(ttl <= remainingUntilDeadline + 250,
                () -> key + " PTTL must not exceed the absolute family deadline");
        return ttl;
    }

    private record RotationOutcome(boolean success, String replacementToken) {

        static RotationOutcome success(String replacementToken) {
            assertNotNull(replacementToken);
            return new RotationOutcome(true, replacementToken);
        }

        static RotationOutcome rejected() {
            return new RotationOutcome(false, null);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

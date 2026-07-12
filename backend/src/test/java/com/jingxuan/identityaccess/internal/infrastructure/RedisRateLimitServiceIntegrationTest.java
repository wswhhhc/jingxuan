package com.jingxuan.identityaccess.internal.infrastructure;

import com.jingxuan.TestContainerImages;
import com.jingxuan.identityaccess.api.RateLimitService;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("integration")
@Testcontainers
class RedisRateLimitServiceIntegrationTest {

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
    void twoServiceInstancesShareTheLimitAndResetTheSameWindow() {
        RedisRateLimitService first = new RedisRateLimitService(redis);
        RedisRateLimitService second = new RedisRateLimitService(redis);
        int limit = 5;
        Duration window = Duration.ofMinutes(1);

        for (int attempt = 1; attempt <= limit; attempt++) {
            RedisRateLimitService service = attempt % 2 == 0 ? second : first;
            RateLimitService.Decision decision = service.consume("login-ip", "203.0.113.8", limit, window);

            assertTrue(decision.allowed());
            assertEquals(attempt, decision.count());
        }

        RateLimitService.Decision rejected = second.consume("login-ip", "203.0.113.8", limit, window);
        assertFalse(rejected.allowed());
        assertEquals(limit, rejected.count());

        assertTrue(first.reset("login-ip", "203.0.113.8", limit, window));
        RateLimitService.Decision afterReset = second.consume("login-ip", "203.0.113.8", limit, window);
        assertTrue(afterReset.allowed());
        assertEquals(1, afterReset.count());
    }

    @Test
    void oneHundredConcurrentAttemptsAllowExactlyTheConfiguredLimit() throws Exception {
        RedisRateLimitService first = new RedisRateLimitService(redis);
        RedisRateLimitService second = new RedisRateLimitService(redis);
        int attempts = 100;
        int limit = 17;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        List<Future<Boolean>> outcomes = new ArrayList<>(attempts);

        try {
            for (int index = 0; index < attempts; index++) {
                RedisRateLimitService service = index % 2 == 0 ? first : second;
                outcomes.add(executor.submit(() -> {
                    start.await();
                    return service.consume(
                            "login-account", "student@example.edu", limit, Duration.ofMinutes(5)).allowed();
                }));
            }

            start.countDown();
            long allowed = 0;
            for (Future<Boolean> outcome : outcomes) {
                if (outcome.get(10, TimeUnit.SECONDS)) {
                    allowed++;
                }
            }

            assertEquals(limit, allowed);
            RateLimitService.Decision finalState = first.inspect(
                    "login-account", "student@example.edu", limit, Duration.ofMinutes(5));
            assertFalse(finalState.allowed());
            assertEquals(limit, finalState.count());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void redisTtlExpiresTheWindowAndRestoresTheQuota() throws Exception {
        RedisRateLimitService service = new RedisRateLimitService(redis);
        Duration window = Duration.ofSeconds(1);

        RateLimitService.Decision allowed = service.consume("public-api", "198.51.100.7", 1, window);
        RateLimitService.Decision rejected = service.consume("public-api", "198.51.100.7", 1, window);

        assertTrue(allowed.allowed());
        assertFalse(rejected.allowed());
        String key = onlyRateLimitKey();
        Long ttlMillis = redis.getExpire(key, TimeUnit.MILLISECONDS);
        assertNotNull(ttlMillis);
        assertTrue(ttlMillis > 0);
        assertTrue(ttlMillis <= window.toMillis());

        awaitExpiration(key, Duration.ofSeconds(5));

        RateLimitService.Decision afterWindow = service.consume("public-api", "198.51.100.7", 1, window);
        assertTrue(afterWindow.allowed());
        assertEquals(1, afterWindow.count());
    }

    private static String onlyRateLimitKey() {
        Set<String> keys = redis.keys(RedisRateLimitService.KEY_PREFIX + "*");
        assertNotNull(keys);
        assertEquals(1, keys.size());
        return keys.iterator().next();
    }

    private static void awaitExpiration(String key, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (!Boolean.TRUE.equals(redis.hasKey(key))) {
                return;
            }
            Thread.sleep(20);
        }
        fail("rate-limit key did not expire within " + timeout);
    }
}

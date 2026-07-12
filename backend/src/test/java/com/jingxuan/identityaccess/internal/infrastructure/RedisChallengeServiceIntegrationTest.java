package com.jingxuan.identityaccess.internal.infrastructure;

import com.jingxuan.TestContainerImages;
import com.jingxuan.identityaccess.api.ChallengePurpose;
import com.jingxuan.identityaccess.api.ChallengeService;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@Testcontainers
class RedisChallengeServiceIntegrationTest {

    private static final Pattern QUESTION = Pattern.compile("(\\d+) ([+-]) (\\d+) = \\?");

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
    void issuedChallengeAcceptsTheCorrectAnswerExactlyOnceAcrossInstances() {
        RedisChallengeService issuer = new RedisChallengeService(redis);
        RedisChallengeService verifier = new RedisChallengeService(redis);
        ChallengeService.IssuedChallenge issued = issuer.issue(ChallengePurpose.LOGIN);
        int answer = solve(issued.question());

        assertTrue(verifier.verifyAndConsume(issued.challengeId(), ChallengePurpose.LOGIN, answer));
        assertFalse(issuer.verifyAndConsume(issued.challengeId(), ChallengePurpose.LOGIN, answer));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(challengeKey(issued))));
    }

    @Test
    void wrongAnswerStillConsumesTheIssuedChallenge() {
        RedisChallengeService service = new RedisChallengeService(redis);
        ChallengeService.IssuedChallenge issued = service.issue(ChallengePurpose.GUEST_COMMENT);
        int answer = solve(issued.question());

        assertFalse(service.verifyAndConsume(
                issued.challengeId(), ChallengePurpose.GUEST_COMMENT, answer + 1));
        assertFalse(service.verifyAndConsume(
                issued.challengeId(), ChallengePurpose.GUEST_COMMENT, answer));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(challengeKey(issued))));
    }

    @Test
    void concurrentConsumptionHasExactlyOneSuccessfulVerifier() throws Exception {
        RedisChallengeService first = new RedisChallengeService(redis);
        RedisChallengeService second = new RedisChallengeService(redis);
        ChallengeService.IssuedChallenge issued = first.issue(ChallengePurpose.LOGIN);
        int answer = solve(issued.question());
        int attempts = 100;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        List<Future<Boolean>> outcomes = new ArrayList<>(attempts);

        try {
            for (int index = 0; index < attempts; index++) {
                RedisChallengeService service = index % 2 == 0 ? first : second;
                outcomes.add(executor.submit(() -> {
                    start.await();
                    return service.verifyAndConsume(issued.challengeId(), ChallengePurpose.LOGIN, answer);
                }));
            }

            start.countDown();
            long succeeded = 0;
            for (Future<Boolean> outcome : outcomes) {
                if (outcome.get(10, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }

            assertEquals(1, succeeded);
            assertFalse(Boolean.TRUE.equals(redis.hasKey(challengeKey(issued))));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void issuedChallengeHasARealRedisTtlNoLongerThanFiveMinutes() {
        RedisChallengeService service = new RedisChallengeService(redis);
        ChallengeService.IssuedChallenge issued = service.issue(ChallengePurpose.LOGIN);

        Long ttlSeconds = redis.getExpire(challengeKey(issued), TimeUnit.SECONDS);

        assertEquals(Duration.ofMinutes(5).toSeconds(), issued.expiresIn());
        assertNotNull(ttlSeconds);
        assertTrue(ttlSeconds > 0);
        assertTrue(ttlSeconds <= Duration.ofMinutes(5).toSeconds());
    }

    private static int solve(String question) {
        Matcher matcher = QUESTION.matcher(question);
        assertTrue(matcher.matches(), () -> "unexpected challenge question: " + question);
        int left = Integer.parseInt(matcher.group(1));
        int right = Integer.parseInt(matcher.group(3));
        return switch (matcher.group(2)) {
            case "+" -> left + right;
            case "-" -> left - right;
            default -> throw new AssertionError("unsupported operator: " + matcher.group(2));
        };
    }

    private static String challengeKey(ChallengeService.IssuedChallenge issued) {
        return RedisChallengeService.KEY_PREFIX + issued.challengeId();
    }
}

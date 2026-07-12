package com.jingxuan.events;

import com.jingxuan.TestContainerImages;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Spring Modulith 2.1 事务事件语义验证。
 *
 * <p>官方来源：
 * https://docs.spring.io/spring-modulith/reference/2.1/events.html#publication-registry</p>
 */
@Tag("integration")
@SpringBootTest(
        classes = PersistentEventPublicationIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=test",
                "spring.flyway.enabled=true",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "spring.modulith.events.completion-mode=delete",
                "spring.modulith.events.republish-outstanding-events-on-restart=false"
        }
)
class PersistentEventPublicationIntegrationTest {

    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(10);
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(TestContainerImages.mysql())
            .withDatabaseName("jingxuan_persistent_event_test")
            .withUsername("jingxuan_persistent_event_test")
            .withPassword("jingxuan_persistent_event_test");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void registerDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @AfterAll
    static void stopMysql() {
        MYSQL.stop();
    }

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private FailedEventPublications failedPublications;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TestEventListener listener;

    @Autowired
    private TransactionTemplate transactions;

    @BeforeEach
    void resetFixture() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS EVENT_LISTENER_PROBE (
                    EVENT_ID VARCHAR(64) NOT NULL,
                    ATTEMPT INT NOT NULL,
                    PRIMARY KEY (EVENT_ID, ATTEMPT)
                )
                """);
        jdbc.update("DELETE FROM EVENT_PUBLICATION");
        jdbc.update("DELETE FROM EVENT_LISTENER_PROBE");
        listener.reset();
    }

    @Test
    void storesPublicationInsideTheBusinessTransactionAndRunsListenerAfterCommit() throws Exception {
        FoundationEvent event = new FoundationEvent(
                UUID.randomUUID().toString(),
                false,
                Thread.currentThread().threadId());

        transactions.executeWithoutResult(status -> {
            events.publishEvent(event);

            assertEquals(1, publicationCount());
            assertEquals(0, listener.attempts());
        });

        assertTrue(listener.awaitSuccess(ASYNC_TIMEOUT));
        awaitCondition(() -> publicationCount() == 0, ASYNC_TIMEOUT);
        assertEquals(1, listener.attempts());
        assertTrue(listener.transactionActive());
        assertTrue(listener.listenerThreadId() != event.publisherThreadId(),
                "@ApplicationModuleListener 必须在异步线程执行");
        assertEquals(1, probeCount(event.id()),
                "监听器自身事务应提交数据库副作用");
    }

    @Test
    void rollsBackPublicationWithTheBusinessTransaction() throws Exception {
        FoundationEvent event = new FoundationEvent(
                UUID.randomUUID().toString(),
                false,
                Thread.currentThread().threadId());

        transactions.executeWithoutResult(status -> {
            events.publishEvent(event);
            assertEquals(1, publicationCount());
            status.setRollbackOnly();
        });

        assertEquals(0, publicationCount());
        assertFalse(listener.awaitSuccess(Duration.ofMillis(500)));
        assertEquals(0, listener.attempts());
        assertEquals(0, probeCount(event.id()));
    }

    @Test
    void retainsFailedPublicationAndAllowsExplicitRetry() throws Exception {
        FoundationEvent event = new FoundationEvent(
                UUID.randomUUID().toString(),
                true,
                Thread.currentThread().threadId());

        transactions.executeWithoutResult(status -> events.publishEvent(event));

        assertTrue(listener.awaitFailure(ASYNC_TIMEOUT));
        awaitCondition(() -> "FAILED".equals(publicationStatus()), ASYNC_TIMEOUT);
        assertEquals(1, publicationCount());
        assertEquals(1, completionAttempts());
        assertTrue(listener.transactionActive());
        assertEquals(0, probeCount(event.id()),
                "监听器抛出异常时其独立事务必须回滚");

        failedPublications.resubmit(ResubmissionOptions.defaults()
                .withBatchSize(1)
                .withMaxInFlight(1));

        assertTrue(listener.awaitSuccess(ASYNC_TIMEOUT));
        awaitCondition(() -> publicationCount() == 0, ASYNC_TIMEOUT);
        assertEquals(2, listener.attempts());
        assertEquals(1, probeCount(event.id()),
                "重试成功后只应提交成功尝试的副作用");
    }

    private int publicationCount() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE LISTENER_ID = ?",
                Integer.class,
                TestEventListener.LISTENER_ID
        );
        return count == null ? 0 : count;
    }

    private String publicationStatus() {
        return jdbc.queryForObject(
                "SELECT STATUS FROM EVENT_PUBLICATION WHERE LISTENER_ID = ?",
                String.class,
                TestEventListener.LISTENER_ID
        );
    }

    private int completionAttempts() {
        Integer attempts = jdbc.queryForObject(
                "SELECT COMPLETION_ATTEMPTS FROM EVENT_PUBLICATION WHERE LISTENER_ID = ?",
                Integer.class,
                TestEventListener.LISTENER_ID
        );
        return attempts == null ? 0 : attempts;
    }

    private int probeCount(String eventId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM EVENT_LISTENER_PROBE WHERE EVENT_ID = ?",
                Integer.class,
                eventId
        );
        return count == null ? 0 : count;
    }

    private static void awaitCondition(BooleanSupplier condition, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(25);
        }
        fail("等待异步事务事件状态变化超时");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(TestEventListener.class)
    static class TestApplication {
    }

    public record FoundationEvent(String id, boolean failFirstAttempt, long publisherThreadId) {
    }

    static class TestEventListener {

        static final String LISTENER_ID = "persistent-event-foundation-test-listener";

        private final AtomicInteger attempts = new AtomicInteger();
        private final AtomicLong listenerThreadId = new AtomicLong(-1);
        private final JdbcTemplate jdbc;
        private volatile CountDownLatch failure = new CountDownLatch(1);
        private volatile CountDownLatch success = new CountDownLatch(1);
        private volatile boolean transactionActive;

        TestEventListener(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @ApplicationModuleListener(id = LISTENER_ID)
        void on(FoundationEvent event) {
            int currentAttempt = attempts.incrementAndGet();
            listenerThreadId.set(Thread.currentThread().threadId());
            transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            jdbc.update(
                    "INSERT INTO EVENT_LISTENER_PROBE (EVENT_ID, ATTEMPT) VALUES (?, ?)",
                    event.id(),
                    currentAttempt);
            if (event.failFirstAttempt() && currentAttempt == 1) {
                failure.countDown();
                throw new IllegalStateException("test-only listener failure");
            }
            success.countDown();
        }

        int attempts() {
            return attempts.get();
        }

        long listenerThreadId() {
            return listenerThreadId.get();
        }

        boolean transactionActive() {
            return transactionActive;
        }

        boolean awaitFailure(Duration timeout) throws InterruptedException {
            return failure.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        boolean awaitSuccess(Duration timeout) throws InterruptedException {
            return success.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        void reset() {
            attempts.set(0);
            listenerThreadId.set(-1);
            transactionActive = false;
            failure = new CountDownLatch(1);
            success = new CountDownLatch(1);
        }
    }
}

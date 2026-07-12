package com.jingxuan.config.event;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PersistentEventRecoveryTaskTest {

    @Test
    void buildsBoundedResubmissionOptionsFromSafeDefaults() {
        FailedEventPublications publications = mock(FailedEventPublications.class);
        PersistentEventRecoveryProperties properties = new PersistentEventRecoveryProperties();
        PersistentEventRecoveryTask task = new PersistentEventRecoveryTask(
                Optional.of(publications), properties);

        task.resubmitFailedPublications();

        ArgumentCaptor<ResubmissionOptions> optionsCaptor =
                ArgumentCaptor.forClass(ResubmissionOptions.class);
        verify(publications).resubmit(optionsCaptor.capture());

        ResubmissionOptions options = optionsCaptor.getValue();
        assertEquals(20, options.getBatchSize());
        assertEquals(5, options.getMaxInFlight());
        assertEquals(Duration.ofMinutes(5), options.getMinAge());
        assertEquals(Duration.ofMinutes(1), properties.getFixedDelay());

        EventPublication neverResubmitted = mock(EventPublication.class);
        EventPublication cooledDown = mock(EventPublication.class);
        EventPublication tooRecent = mock(EventPublication.class);
        Instant now = Instant.now();
        when(neverResubmitted.getLastResubmissionDate()).thenReturn(null);
        when(cooledDown.getLastResubmissionDate()).thenReturn(now.minus(Duration.ofMinutes(6)));
        when(tooRecent.getLastResubmissionDate()).thenReturn(now.minus(Duration.ofMinutes(4)));

        assertTrue(options.getFilter().test(neverResubmitted));
        assertTrue(options.getFilter().test(cooledDown));
        assertFalse(options.getFilter().test(tooRecent));
    }

    @Test
    void disabledRecoveryDoesNotTouchFailedPublications() {
        FailedEventPublications publications = mock(FailedEventPublications.class);
        PersistentEventRecoveryProperties properties = new PersistentEventRecoveryProperties();
        properties.setEnabled(false);
        PersistentEventRecoveryTask task = new PersistentEventRecoveryTask(
                Optional.of(publications), properties);

        task.resubmitFailedPublications();

        verifyNoInteractions(publications);
    }

    @Test
    void failedCycleIsSanitizedAndDoesNotBlockTheNextRetry() {
        String sensitivePayload = "student-private-event-payload";
        FailedEventPublications publications = mock(FailedEventPublications.class);
        PersistentEventRecoveryProperties properties = new PersistentEventRecoveryProperties();
        PersistentEventRecoveryTask task = new PersistentEventRecoveryTask(
                Optional.of(publications), properties);
        doThrow(new IllegalStateException(sensitivePayload))
                .doNothing()
                .when(publications)
                .resubmit(org.mockito.ArgumentMatchers.any(ResubmissionOptions.class));

        Logger logger = (Logger) LoggerFactory.getLogger(PersistentEventRecoveryTask.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertDoesNotThrow(task::resubmitFailedPublications);
            assertDoesNotThrow(task::resubmitFailedPublications);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        verify(publications, times(2))
                .resubmit(org.mockito.ArgumentMatchers.any(ResubmissionOptions.class));
        assertTrue(appender.list.stream().anyMatch(event -> event.getLevel() == Level.ERROR));
        assertTrue(appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .anyMatch(message -> message.contains(IllegalStateException.class.getName())));
        assertFalse(appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .anyMatch(message -> message.contains(sensitivePayload)));
        assertTrue(appender.list.stream().allMatch(event -> event.getThrowableProxy() == null),
                "恢复日志不得附带可能包含事件载荷的异常消息或堆栈");
    }

    @Test
    void missingPublicationInfrastructureIsANoopForToolingContexts() {
        PersistentEventRecoveryProperties properties = new PersistentEventRecoveryProperties();
        PersistentEventRecoveryTask task = new PersistentEventRecoveryTask(
                Optional.empty(), properties);

        assertDoesNotThrow(task::resubmitFailedPublications);
    }

    @Test
    void rejectsSettingsThatCouldCreateUnboundedRecoveryPressure() {
        PersistentEventRecoveryProperties properties = new PersistentEventRecoveryProperties();
        properties.setBatchSize(1001);
        properties.setMaxInFlight(101);
        properties.setMinAge(Duration.ofDays(31));
        properties.setFixedDelay(Duration.ofSeconds(1));

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var messages = factory.getValidator().validate(properties).stream()
                    .map(violation -> violation.getMessage())
                    .toList();

            assertTrue(messages.stream().anyMatch(message -> message.contains("1000")));
            assertTrue(messages.stream().anyMatch(message -> message.contains("100")));
            assertTrue(messages.stream().anyMatch(message -> message.contains("30 天")));
            assertTrue(messages.stream().anyMatch(message -> message.contains("10 秒")));
        }
    }

    @Test
    void schedulerUsesTheConfiguredFixedDelay() throws Exception {
        Method method = PersistentEventRecoveryTask.class
                .getDeclaredMethod("resubmitFailedPublications");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals("${jingxuan.events.recovery.fixed-delay:PT1M}",
                scheduled.fixedDelayString());
    }

}

package com.jingxuan.config.event;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentEventRecoveryContractTest {

    @Test
    void configuresAllOfficialModulith21StalenessKeys() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));
        PropertySource<?> application = sources.get(0);

        assertNotNull(application.getProperty("spring.modulith.events.staleness.published"));
        assertNotNull(application.getProperty("spring.modulith.events.staleness.processing"));
        assertNotNull(application.getProperty("spring.modulith.events.staleness.resubmitted"));
        assertNotNull(application.getProperty("spring.modulith.events.staleness.check-intervall"));
        assertEquals("${EVENT_RECOVERY_ENABLED:true}", application.getProperty(
                "spring.modulith.events.republish-outstanding-events-on-restart"));
        assertEquals("${EVENT_RECOVERY_ENABLED:true}",
                application.getProperty("jingxuan.events.recovery.enabled"));
        assertEquals("${EVENT_RECOVERY_BATCH_SIZE:20}",
                application.getProperty("jingxuan.events.recovery.batch-size"));
        assertEquals("${EVENT_RECOVERY_MAX_IN_FLIGHT:5}",
                application.getProperty("jingxuan.events.recovery.max-in-flight"));
        assertEquals("${EVENT_RECOVERY_MIN_AGE:PT5M}",
                application.getProperty("jingxuan.events.recovery.min-age"));
        assertEquals("${EVENT_RECOVERY_FIXED_DELAY:PT1M}",
                application.getProperty("jingxuan.events.recovery.fixed-delay"));
        assertEquals("WARN", application.getProperty(
                "logging.level.org.springframework.modulith.events.core.DefaultEventPublicationRegistry"));
    }

    @Test
    void enablesSchedulingWithoutRedeclaringAsyncInfrastructure() {
        assertTrue(PersistentEventRecoveryConfiguration.class
                .isAnnotationPresent(EnableScheduling.class));
        assertFalse(PersistentEventRecoveryConfiguration.class
                .isAnnotationPresent(EnableAsync.class));
    }

    @Test
    void disabledPropertyRemovesBothTheTaskAndItsSchedulingInfrastructure() {
        ConditionalOnProperty taskCondition = PersistentEventRecoveryTask.class
                .getAnnotation(ConditionalOnProperty.class);
        ConditionalOnProperty schedulingCondition = PersistentEventRecoveryConfiguration.class
                .getAnnotation(ConditionalOnProperty.class);

        assertEquals("enabled", taskCondition.name()[0]);
        assertEquals("enabled", schedulingCondition.name()[0]);

        new ApplicationContextRunner()
                .withUserConfiguration(
                        PersistentEventRecoveryConfiguration.class,
                        PersistentEventRecoveryTask.class)
                .withBean(PersistentEventRecoveryProperties.class)
                .withPropertyValues("jingxuan.events.recovery.enabled=false")
                .run(context -> {
                    assertFalse(context.containsBean("persistentEventRecoveryTask"));
                    assertFalse(context.containsBean(
                            "org.springframework.context.annotation.internalScheduledAnnotationProcessor"));
                });
    }

    @Test
    void productionSourceDoesNotDeclareEnableAsync() throws Exception {
        try (Stream<Path> paths = Files.walk(Path.of("src/main/java"))) {
            List<Path> offenders = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path, StandardCharsets.UTF_8)
                                    .contains("import org.springframework.scheduling.annotation.EnableAsync;");
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(offenders.isEmpty(), "不得重复启用全局异步基础设施: " + offenders);
        }
    }

    @Test
    void operationsGuideMakesTheSingleCoordinatorConstraintExplicit() throws Exception {
        String guide = Files.readString(Path.of("../docs/运维手册.md"), StandardCharsets.UTF_8);

        assertTrue(guide.contains("EVENT_RECOVERY_ENABLED"));
        assertTrue(guide.contains("多实例"));
        assertTrue(guide.contains("只能有一个"));
        assertTrue(guide.contains("自动重放"));
    }
}

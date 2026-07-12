package com.jingxuan.config.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 单实例失败事件重放任务。
 *
 * <p>这里只记录异常类型，不记录异常消息、堆栈或事件对象，避免领域事件载荷进入日志。
 */
@Component
@ConditionalOnProperty(
        prefix = "jingxuan.events.recovery",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PersistentEventRecoveryTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentEventRecoveryTask.class);

    private final Optional<FailedEventPublications> failedPublications;
    private final PersistentEventRecoveryProperties properties;

    PersistentEventRecoveryTask(
            Optional<FailedEventPublications> failedPublications,
            PersistentEventRecoveryProperties properties) {
        this.failedPublications = failedPublications;
        this.properties = properties;
    }

    /**
     * 使用 fixed delay 保证同一实例内上一轮返回后才开始计算下一轮延迟。
     */
    @Scheduled(fixedDelayString = "${jingxuan.events.recovery.fixed-delay:PT1M}")
    void resubmitFailedPublications() {
        if (!properties.isEnabled() || failedPublications.isEmpty()) {
            return;
        }

        try {
            failedPublications.orElseThrow().resubmit(properties.toResubmissionOptions());
        } catch (Exception exception) {
            LOGGER.error(
                    "持久化事件恢复周期执行失败，将在下一周期重试（异常类型={}）。",
                    exception.getClass().getName());
        }
    }
}

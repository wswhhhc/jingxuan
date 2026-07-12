package com.jingxuan.config.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 为单实例持久化事件恢复启用调度基础设施。
 *
 * <p>Spring Modulith starter 已按需启用异步处理，这里不得重复声明 {@code @EnableAsync}。
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        prefix = "jingxuan.events.recovery",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PersistentEventRecoveryConfiguration {
}

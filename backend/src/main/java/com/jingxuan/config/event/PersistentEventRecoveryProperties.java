package com.jingxuan.config.event;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.Instant;

/**
 * 单实例持久化事件恢复参数。
 *
 * <p>Spring Modulith 2.1 的 {@link ResubmissionOptions} 原生支持批量大小、最大并发重放数和最小事件年龄：
 * https://docs.spring.io/spring-modulith/docs/2.1.0/api/org/springframework/modulith/events/ResubmissionOptions.html
 */
@Validated
@ConfigurationProperties(prefix = "jingxuan.events.recovery")
public class PersistentEventRecoveryProperties {

    private static final Duration MIN_EVENT_AGE = Duration.ofSeconds(1);
    private static final Duration MAX_EVENT_AGE = Duration.ofDays(30);
    private static final Duration MIN_FIXED_DELAY = Duration.ofSeconds(10);
    private static final Duration MAX_FIXED_DELAY = Duration.ofDays(1);

    private boolean enabled = true;

    @Min(1)
    @Max(1000)
    private int batchSize = 20;

    @Min(1)
    @Max(100)
    private int maxInFlight = 5;

    @NotNull
    private Duration minAge = Duration.ofMinutes(5);

    @NotNull
    private Duration fixedDelay = Duration.ofMinutes(1);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxInFlight() {
        return maxInFlight;
    }

    public void setMaxInFlight(int maxInFlight) {
        this.maxInFlight = maxInFlight;
    }

    public Duration getMinAge() {
        return minAge;
    }

    public void setMinAge(Duration minAge) {
        this.minAge = minAge;
    }

    public Duration getFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(Duration fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    @AssertTrue(message = "事件恢复最小年龄必须在 1 秒到 30 天之间")
    public boolean isMinAgeWithinBounds() {
        return isWithin(minAge, MIN_EVENT_AGE, MAX_EVENT_AGE);
    }

    @AssertTrue(message = "事件恢复固定延迟必须在 10 秒到 1 天之间")
    public boolean isFixedDelayWithinBounds() {
        return isWithin(fixedDelay, MIN_FIXED_DELAY, MAX_FIXED_DELAY);
    }

    @AssertTrue(message = "事件恢复最大 in-flight 数不得超过批量大小")
    public boolean isMaxInFlightWithinBatch() {
        return maxInFlight <= batchSize;
    }

    ResubmissionOptions toResubmissionOptions() {
        Instant lastEligibleResubmission = Instant.now().minus(minAge);

        return ResubmissionOptions.defaults()
                .withBatchSize(batchSize)
                .withMaxInFlight(maxInFlight)
                .withMinAge(minAge)
                .withFilter(publication -> isOutsideRetryBackoff(
                        publication, lastEligibleResubmission));
    }

    private static boolean isWithin(Duration value, Duration minimum, Duration maximum) {
        return value != null && value.compareTo(minimum) >= 0 && value.compareTo(maximum) <= 0;
    }

    private static boolean isOutsideRetryBackoff(
            EventPublication publication,
            Instant lastEligibleResubmission) {
        Instant lastResubmission = publication.getLastResubmissionDate();
        return lastResubmission == null || !lastResubmission.isAfter(lastEligibleResubmission);
    }
}

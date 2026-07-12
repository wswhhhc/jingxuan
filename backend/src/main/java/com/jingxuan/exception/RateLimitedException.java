package com.jingxuan.exception;

/** 旧接口也可携带真实窗口剩余时间的限流异常。 */
public final class RateLimitedException extends BusinessException {

    private final long retryAfterSeconds;

    public RateLimitedException(String message, long retryAfterSeconds) {
        super(429, message);
        if (retryAfterSeconds < 1) {
            throw new IllegalArgumentException("Retry-After 必须至少为 1 秒");
        }
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

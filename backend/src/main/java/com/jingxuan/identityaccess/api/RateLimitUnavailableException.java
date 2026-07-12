package com.jingxuan.identityaccess.api;

/** Redis 限流存储不可用或返回不可信状态。 */
public final class RateLimitUnavailableException extends RuntimeException {

    public RateLimitUnavailableException() {
        super("限流服务暂时不可用，请稍后重试");
    }
}

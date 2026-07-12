package com.jingxuan.identityaccess.api;

/** 注册邮箱验证码限流响应。 */
public final class EmailVerificationRateLimitedException extends IdentityAccessProblemException {

    public EmailVerificationRateLimitedException(long retryAfterSeconds) {
        super(429, "RATE_LIMITED", "邮箱验证码请求过于频繁，请稍后再试", retryAfterSeconds);
    }
}

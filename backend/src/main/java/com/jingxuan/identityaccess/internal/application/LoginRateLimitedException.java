package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.identityaccess.api.IdentityAccessProblemException;

final class LoginRateLimitedException extends IdentityAccessProblemException {

    LoginRateLimitedException(long retryAfterSeconds) {
        super(429, "RATE_LIMITED", "登录尝试过于频繁，请稍后再试", retryAfterSeconds);
    }
}

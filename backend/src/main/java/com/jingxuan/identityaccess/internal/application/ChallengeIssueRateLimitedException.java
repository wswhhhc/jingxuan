package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.identityaccess.api.IdentityAccessProblemException;

public final class ChallengeIssueRateLimitedException extends IdentityAccessProblemException {

    public ChallengeIssueRateLimitedException(long retryAfterSeconds) {
        super(429, "RATE_LIMITED", "安全校验创建过于频繁，请稍后再试", retryAfterSeconds);
    }
}

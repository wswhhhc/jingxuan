package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.identityaccess.api.IdentityAccessProblemException;

final class LoginChallengeInvalidException extends IdentityAccessProblemException {

    LoginChallengeInvalidException() {
        super(401, "LOGIN_CHALLENGE_INVALID", "登录安全校验无效或已过期");
    }
}

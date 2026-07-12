package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.identityaccess.api.IdentityAccessProblemException;

final class LoginChallengeRequiredException extends IdentityAccessProblemException {

    LoginChallengeRequiredException() {
        super(401, "LOGIN_CHALLENGE_REQUIRED", "请先完成登录安全校验");
    }
}

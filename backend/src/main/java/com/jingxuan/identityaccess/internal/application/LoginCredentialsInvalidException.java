package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.identityaccess.api.IdentityAccessProblemException;

public final class LoginCredentialsInvalidException extends IdentityAccessProblemException {

    public LoginCredentialsInvalidException() {
        super(401, "LOGIN_CREDENTIALS_INVALID", "用户名或密码错误");
    }
}

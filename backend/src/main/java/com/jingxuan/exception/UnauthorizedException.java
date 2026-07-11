package com.jingxuan.exception;

import java.io.Serial;

/**
 * 认证/授权异常
 */
public class UnauthorizedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    public UnauthorizedException(String message) {
        super(message);
        this.code = 401;
    }

    public UnauthorizedException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public boolean isAuthFailed() {
        return code == 401;
    }

    public boolean isAccessDenied() {
        return code == 403;
    }
}

package com.jingxuan.exception;

import java.io.Serial;

/**
 * 资源不存在异常
 */
public class NotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    public NotFoundException(String message) {
        super(message);
        this.code = 404;
    }

    public NotFoundException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

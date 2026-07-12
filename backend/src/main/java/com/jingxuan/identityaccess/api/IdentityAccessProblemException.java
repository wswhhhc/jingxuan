package com.jingxuan.identityaccess.api;

/** identity-access v1 的稳定 Problem Details 错误契约。 */
public abstract class IdentityAccessProblemException extends RuntimeException {

    private final int status;
    private final String problemCode;
    private final Long retryAfterSeconds;

    protected IdentityAccessProblemException(int status, String problemCode, String message) {
        this(status, problemCode, message, null);
    }

    protected IdentityAccessProblemException(int status, String problemCode, String message, Long retryAfterSeconds) {
        super(message);
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("status 必须是 4xx 或 5xx");
        }
        if (problemCode == null || problemCode.isBlank()) {
            throw new IllegalArgumentException("problemCode 不能为空");
        }
        if (retryAfterSeconds != null && retryAfterSeconds < 1) {
            throw new IllegalArgumentException("Retry-After 必须至少为 1 秒");
        }
        this.status = status;
        this.problemCode = problemCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int status() {
        return status;
    }

    public String problemCode() {
        return problemCode;
    }

    public Long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}

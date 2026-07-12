package com.jingxuan.identityaccess.api;

/** challenge 的 Redis 存储不可用，调用方必须停止而不能降级绕过。 */
public final class ChallengeUnavailableException extends IdentityAccessProblemException {

    public ChallengeUnavailableException() {
        super(503, "CHALLENGE_UNAVAILABLE", "登录安全校验服务暂时不可用，请稍后重试", 1L);
    }
}

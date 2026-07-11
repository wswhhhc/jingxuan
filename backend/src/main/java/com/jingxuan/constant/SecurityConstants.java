package com.jingxuan.constant;

/**
 * 安全相关常量
 */
public interface SecurityConstants {

    /** 请求头中Token的Key */
    String TOKEN_HEADER = "Authorization";

    /** Token前缀 */
    String TOKEN_PREFIX = "Bearer ";

    /** 用户ID claim key */
    String CLAIM_USER_ID = "userId";

    /** 用户名 claim key */
    String CLAIM_USERNAME = "username";

    /** 角色 claim key */
    String CLAIM_ROLE = "role";

    /** remember-me：7天 */
    long REMEMBER_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    /** 默认过期：24小时 */
    long DEFAULT_EXPIRATION = 24 * 60 * 60 * 1000L;

    /** Redis缓存前缀 */
    String TOKEN_BLACKLIST_PREFIX = "jingxuan:token:blacklist:";
}

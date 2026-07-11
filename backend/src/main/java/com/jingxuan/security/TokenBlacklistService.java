package com.jingxuan.security;

public interface TokenBlacklistService {

    void blacklist(String token, long ttlMillis);

    boolean isBlacklisted(String token);
}

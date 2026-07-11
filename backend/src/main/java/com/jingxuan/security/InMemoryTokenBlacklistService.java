package com.jingxuan.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    /**
     * 最大缓存 10000 条，写入后 24 小时自动过期（即使 token 未再被查询）
     */
    private final Cache<String, Long> blacklist = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    @Override
    public void blacklist(String token, long ttlMillis) {
        if (token == null || token.isBlank()) {
            return;
        }
        blacklist.put(token, System.currentTimeMillis() + Math.max(ttlMillis, 1));
    }

    @Override
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Long expireAt = blacklist.getIfPresent(token);
        if (expireAt == null) {
            return false;
        }
        // 即使 Caffeine 缓存条目还在，也要检查实际过期时间
        if (expireAt < System.currentTimeMillis()) {
            blacklist.invalidate(token);
            return false;
        }
        return true;
    }
}

package com.jingxuan.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jingxuan.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private static final int LIMIT = 20;
    private static final long WINDOW_MS = 1000L;

    private final ObjectMapper objectMapper;

    /**
     * 按 IP+URI 限流，窗口 1 秒后自动过期，最大缓存 10000 条
     */
    private final Cache<String, Counter> counters = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .build();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/public/") || path.startsWith("/comment/list/")
                || path.equals("/comment/add") || path.equals("/api/comment/add"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        long now = System.currentTimeMillis();

        Counter counter = counters.get(key, k -> new Counter(now));
        if (now - counter.windowStart >= WINDOW_MS) {
            counter.windowStart = now;
            counter.count = 1;
        } else {
            counter.count++;
        }

        if (counter.count > LIMIT) {
            writeLimited(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeLimited(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.fail(429, "访问过于频繁，请稍后再试"));
    }

    private static class Counter {
        private long windowStart;
        private int count;

        private Counter(long windowStart) {
            this.windowStart = windowStart;
            this.count = 1;
        }
    }
}

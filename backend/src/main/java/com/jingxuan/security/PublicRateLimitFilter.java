package com.jingxuan.security;

import tools.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jingxuan.api.ApiPaths;
import com.jingxuan.api.ProblemDetails;
import com.jingxuan.api.RequestIdFilter;
import com.jingxuan.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final TrustedProxyClientIpResolver clientIpResolver;
    private final int limit;

    /**
     * 按可信客户端 IP + URI 限流，窗口按配置自动过期，最大缓存 10000 条。
     */
    private final Cache<String, AtomicInteger> counters;

    public PublicRateLimitFilter(
            ObjectMapper objectMapper,
            TrustedProxyClientIpResolver clientIpResolver,
            @Value("${jingxuan.public-rate-limit.limit:20}") int limit,
            @Value("${jingxuan.public-rate-limit.window-ms:1000}") long windowMs) {
        this.objectMapper = objectMapper;
        this.clientIpResolver = clientIpResolver;
        this.limit = limit;
        this.counters = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(windowMs, TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean v1Showcase = ApiPaths.isShowcaseV1Operation(request.getMethod(), path);
        return !(path.startsWith("/api/public/") || path.startsWith("/api/comment/list/")
                || path.equals("/api/comment/add") || v1Showcase);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = clientIpResolver.resolve(request) + ":" + request.getRequestURI();
        AtomicInteger counter = counters.get(key, ignored -> new AtomicInteger());
        if (counter.incrementAndGet() > limit) {
            writeLimited(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeLimited(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (ApiPaths.isV1(request.getRequestURI())) {
            Object requestId = request.getAttribute(RequestIdFilter.ATTRIBUTE);
            ProblemDetails problem = ProblemDetails.of(429, "RATE_LIMITED", "访问过于频繁，请稍后再试",
                    request.getRequestURI(), requestId == null ? request.getHeader(RequestIdFilter.HEADER)
                            : requestId.toString());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), problem);
            return;
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.fail(429, "访问过于频繁，请稍后再试"));
    }

    /** 清空当前限流窗口，供测试隔离和受控维护使用。 */
    public void clearCounters() {
        counters.invalidateAll();
    }
}


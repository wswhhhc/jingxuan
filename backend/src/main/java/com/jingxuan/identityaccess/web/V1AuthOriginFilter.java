package com.jingxuan.identityaccess.web;

import com.jingxuan.security.RestAccessDeniedHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/** 在任何认证副作用前拒绝非同源的 Cookie 会话请求。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class V1AuthOriginFilter extends OncePerRequestFilter {

    private static final List<RequestMatcher> PROTECTED_REQUESTS = List.of(
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/auth/login"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/auth/refresh"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/auth/logout")
    );

    private final RestAccessDeniedHandler accessDeniedHandler;
    private final Environment environment;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PROTECTED_REQUESTS.stream().noneMatch(matcher -> matcher.matches(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 开发环境（Vite 5173 != 后端 8080，不同源）放行同源检查
        if (isDevelopment()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!hasSameOrigin(request)) {
            accessDeniedHandler.handle(request, response,
                    new AccessDeniedException("认证请求必须来自同源页面"));
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** 判断当前是否为开发/测试环境 */
    private boolean isDevelopment() {
        return environment.acceptsProfiles(Profiles.of("dev", "test"));
    }

    private static boolean hasSameOrigin(HttpServletRequest request) {
        String originValue = singleOriginHeader(request);
        if (originValue == null) {
            return false;
        }

        URI origin;
        try {
            origin = URI.create(originValue);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        String scheme = origin.getScheme();
        String host = origin.getHost();
        if (scheme == null || host == null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || origin.getRawUserInfo() != null
                || hasText(origin.getRawPath())
                || origin.getRawQuery() != null
                || origin.getRawFragment() != null) {
            return false;
        }

        return scheme.equalsIgnoreCase(request.getScheme())
                && host.equalsIgnoreCase(request.getServerName())
                && effectivePort(scheme, origin.getPort()) == request.getServerPort();
    }

    private static String singleOriginHeader(HttpServletRequest request) {
        Enumeration<String> origins = request.getHeaders(HttpHeaders.ORIGIN);
        if (origins == null || !origins.hasMoreElements()) {
            return null;
        }
        String origin = origins.nextElement();
        if (origins.hasMoreElements() || origin == null) {
            return null;
        }
        String normalized = origin.trim();
        if (normalized.isEmpty() || "null".equals(normalized.toLowerCase(Locale.ROOT))) {
            return null;
        }
        return normalized;
    }

    private static int effectivePort(String scheme, int explicitPort) {
        if (explicitPort >= 0) {
            return explicitPort;
        }
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}

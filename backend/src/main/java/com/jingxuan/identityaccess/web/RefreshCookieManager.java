package com.jingxuan.identityaccess.web;

import com.jingxuan.exception.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/** 负责 refresh Cookie 的唯一读写边界。 */
@Component
public class RefreshCookieManager {

    public static final String COOKIE_NAME = "jingxuan_refresh";
    public static final String COOKIE_PATH = "/api/v1/auth";
    public static final String OPENAPI_COOKIE_DESCRIPTION =
            "jingxuan_refresh; HttpOnly; SameSite=Strict; Path=/api/v1/auth; Secure=false";
    public static final String OPENAPI_CLEAR_COOKIE_DESCRIPTION =
            OPENAPI_COOKIE_DESCRIPTION + "; Max-Age=0";

    public String require(HttpServletRequest request) {
        return find(request).orElseThrow(() ->
                new UnauthorizedException("登录状态已失效，请重新登录"));
    }

    public Optional<String> find(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        String refreshToken = null;
        for (Cookie cookie : cookies) {
            if (!COOKIE_NAME.equals(cookie.getName())) {
                continue;
            }
            if (refreshToken != null) {
                return Optional.empty();
            }
            refreshToken = cookie.getValue();
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(refreshToken);
    }

    public void write(HttpServletResponse response, String refreshToken, long expiresInSeconds) {
        ResponseCookie cookie = baseCookie(refreshToken)
                .maxAge(Duration.ofSeconds(Math.max(expiresInSeconds, 0)))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .path(COOKIE_PATH)
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict");
    }
}

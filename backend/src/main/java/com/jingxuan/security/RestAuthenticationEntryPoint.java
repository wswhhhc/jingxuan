package com.jingxuan.security;

import tools.jackson.databind.ObjectMapper;
import com.jingxuan.api.ProblemDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        String requestId = request.getAttribute(com.jingxuan.api.RequestIdFilter.ATTRIBUTE) != null
                ? request.getAttribute(com.jingxuan.api.RequestIdFilter.ATTRIBUTE).toString() : null;
        ProblemDetails problem = new ProblemDetails(
                "about:blank",
                "未认证",
                401,
                "未登录或登录已过期",
                request.getRequestURI(),
                "UNAUTHENTICATED",
                requestId,
                Map.of()
        );
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}


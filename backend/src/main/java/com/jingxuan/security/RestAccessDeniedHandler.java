package com.jingxuan.security;

import tools.jackson.databind.ObjectMapper;
import com.jingxuan.api.ProblemDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        String requestId = request.getAttribute(com.jingxuan.api.RequestIdFilter.ATTRIBUTE) != null
                ? request.getAttribute(com.jingxuan.api.RequestIdFilter.ATTRIBUTE).toString() : null;
        ProblemDetails problem = new ProblemDetails(
                "about:blank",
                "禁止访问",
                403,
                "权限不足，无法访问",
                request.getRequestURI(),
                "FORBIDDEN",
                requestId,
                Map.of()
        );
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}


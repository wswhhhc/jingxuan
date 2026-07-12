package com.jingxuan.security;

import com.jingxuan.api.ProblemDetails;
import com.jingxuan.api.RequestIdFilter;
import com.jingxuan.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 按 API 版本写出安全层错误，避免破坏旧接口的 Result 契约。 */
final class SecurityErrorResponseWriter {

    private static final String V1_PREFIX = "/api/v1";

    private SecurityErrorResponseWriter() {
    }

    static void write(HttpServletRequest request,
                      HttpServletResponse response,
                      ObjectMapper objectMapper,
                      int status,
                      String code,
                      String detail,
                      Result<Void> legacyBody) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (isV1Request(request)) {
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            ProblemDetails details = ProblemDetails.of(status, code, detail,
                    request.getRequestURI(), requestId(request));
            objectMapper.writeValue(response.getWriter(), details);
            return;
        }

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), legacyBody);
    }

    private static boolean isV1Request(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return V1_PREFIX.equals(uri) || uri.startsWith(V1_PREFIX + "/");
    }

    private static String requestId(HttpServletRequest request) {
        Object attribute = request.getAttribute(RequestIdFilter.ATTRIBUTE);
        return attribute == null ? request.getHeader(RequestIdFilter.HEADER) : attribute.toString();
    }
}

package com.jingxuan.exception;

import com.jingxuan.api.ProblemDetails;
import com.jingxuan.api.RequestIdFilter;
import com.jingxuan.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler - 异常兜底测试")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("数据库连接失败时返回统一错误信息")
    void databaseConnectionFailure() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/public/works");

        Result<Void> result = handler.handleDatabaseTimeoutException(
                new DataAccessResourceFailureException("Connection timed out"),
                request);

        assertEquals(500, result.getCode());
        assertEquals("数据库连接超时，请稍后重试", result.getMessage());
    }

    @Test
    @DisplayName("数据库查询超时时返回统一错误信息")
    void databaseQueryTimeout() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/admin/log/list");

        Result<Void> result = handler.handleDatabaseTimeoutException(
                new QueryTimeoutException("Query timeout"),
                request);

        assertEquals(500, result.getCode());
        assertEquals("数据库连接超时，请稍后重试", result.getMessage());
    }

    @Test
    @DisplayName("缺失的静态上传文件返回 404 而不是服务器错误")
    void missingStaticResource() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/uploads/missing.jpg");
        ResponseEntity<?> response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/uploads/missing.jpg", "missing.jpg"), request);

        Result<?> result = (Result<?>) response.getBody();
        assertEquals(404, response.getStatusCode().value());
        assertEquals(404, result.getCode());
        assertEquals("请求的资源不存在", result.getMessage());
    }

    @Test
    @DisplayName("V1 缺失资源返回 Problem Details")
    void missingV1Resource() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/missing");
        when(request.getAttribute(RequestIdFilter.ATTRIBUTE)).thenReturn("request-404");

        ResponseEntity<?> response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/api/v1/missing", "missing"), request);

        ProblemDetails details = (ProblemDetails) response.getBody();
        assertEquals(404, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertEquals("NOT_FOUND", details.code());
        assertEquals("request-404", details.requestId());
    }
}

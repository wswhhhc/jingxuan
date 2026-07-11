package com.jingxuan.exception;

import com.jingxuan.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;

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
        when(request.getRequestURI()).thenReturn("/public/works");

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
        when(request.getRequestURI()).thenReturn("/admin/log/list");

        Result<Void> result = handler.handleDatabaseTimeoutException(
                new QueryTimeoutException("Query timeout"),
                request);

        assertEquals(500, result.getCode());
        assertEquals("数据库连接超时，请稍后重试", result.getMessage());
    }
}

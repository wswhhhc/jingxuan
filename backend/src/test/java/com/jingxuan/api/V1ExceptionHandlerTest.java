package com.jingxuan.api;

import com.jingxuan.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V1ExceptionHandlerTest {

    private final V1ExceptionHandler handler = new V1ExceptionHandler();
    private final HttpServletRequest request = request("/api/v1/works/1");

    @Test
    void mapsBusinessExceptionToProblemDetails() {
        ResponseEntity<ProblemDetails> response = handler.business(
                new BusinessException(409, "作品版本冲突"), request);

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("BUSINESS_ERROR", response.getBody().code());
        assertEquals("/api/v1/works/1", response.getBody().instance());
        assertEquals("请求-123", response.getBody().requestId());
    }

    @Test
    void mapsUnexpectedExceptionWithoutLeakingDetails() {
        ResponseEntity<ProblemDetails> response = handler.unexpected(
                new IllegalStateException("database password"), request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("服务器内部错误，请稍后重试", response.getBody().detail());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
    }

    private static HttpServletRequest request(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getAttribute(RequestIdFilter.ATTRIBUTE)).thenReturn("请求-123");
        return request;
    }
}

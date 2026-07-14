package com.jingxuan.api;

import com.jingxuan.exception.BusinessException;
import com.jingxuan.identityaccess.api.RateLimitUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

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
        assertEquals("request-123", response.getBody().requestId());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
    }

    @Test
    void mapsUnexpectedExceptionWithoutLeakingDetails() {
        ResponseEntity<ProblemDetails> response = handler.unexpected(
                new IllegalStateException("database password"), request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("服务器内部错误，请稍后重试", response.getBody().detail());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
    }

    @Test
    void mapsValidationExceptionToProblemDetailsMediaType() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "title", "标题不能为空"));
        Method method = V1ExceptionHandlerTest.class.getDeclaredMethod("validatedMethod", Object.class);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new MethodParameter(method, 0), bindingResult);

        ResponseEntity<ProblemDetails> response = handler.validation(exception, request);

        assertEquals(422, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertEquals("VALIDATION_ERROR", response.getBody().code());
        assertEquals("标题不能为空", response.getBody().fieldErrors().get("title"));
    }

    @Test
    void mapsUnsupportedMediaTypeToProblemDetails() {
        ResponseEntity<ProblemDetails> response = handler.mediaTypeNotSupported(request);

        assertEquals(415, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertEquals("UNSUPPORTED_MEDIA_TYPE", response.getBody().code());
    }

    @Test
    void mapsNotAcceptableToProblemDetails() {
        ResponseEntity<ProblemDetails> response = handler.mediaTypeNotAcceptable(request);

        assertEquals(406, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertEquals("NOT_ACCEPTABLE", response.getBody().code());
    }

    @Test
    void mapsUnavailableRateLimitStorageToServiceUnavailableProblem() {
        ResponseEntity<ProblemDetails> response = handler.rateLimitUnavailable(
                new RateLimitUnavailableException(), request);

        assertEquals(503, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertEquals("RATE_LIMIT_UNAVAILABLE", response.getBody().code());
    }

    @SuppressWarnings("unused")
    private void validatedMethod(Object request) {
    }

    private static HttpServletRequest request(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getAttribute(RequestIdFilter.ATTRIBUTE)).thenReturn("request-123");
        return request;
    }
}

package com.jingxuan.exception;

import com.jingxuan.api.ProblemDetails;
import com.jingxuan.api.RequestIdFilter;
import com.jingxuan.common.Result;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalErrorControllerTest {

    private final GlobalErrorController controller = new GlobalErrorController();

    @Test
    void v1ErrorDispatchUsesProblemDetails() {
        MockHttpServletRequest request = errorRequest(404, "/api/v1/missing", "request-404");

        var response = controller.handleError(request);

        ProblemDetails details = (ProblemDetails) response.getBody();
        assertEquals(404, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertEquals("NOT_FOUND", details.code());
        assertEquals("/api/v1/missing", details.instance());
        assertEquals("request-404", details.requestId());
    }

    @Test
    void legacyErrorDispatchKeepsResultEnvelope() {
        MockHttpServletRequest request = errorRequest(404, "/api/student/missing", "legacy-404");

        var response = controller.handleError(request);

        Result<?> result = (Result<?>) response.getBody();
        assertEquals(404, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertEquals(404, result.getCode());
    }

    @ParameterizedTest
    @CsvSource({
            "400,BAD_REQUEST,请求无效",
            "401,UNAUTHENTICATED,未登录或登录已过期",
            "403,FORBIDDEN,权限不足，无法访问",
            "405,METHOD_NOT_ALLOWED,请求方法不支持",
            "406,NOT_ACCEPTABLE,无法生成客户端可接受的响应格式",
            "415,UNSUPPORTED_MEDIA_TYPE,请求媒体类型不受支持",
            "500,INTERNAL_ERROR,服务器内部错误，请稍后重试"
    })
    void v1ErrorDispatchMapsStatusToMachineReadableProblem(
            int status, String code, String detail) {
        MockHttpServletRequest request = errorRequest(status, "/api/v1/error-case", "request-error");

        var response = controller.handleError(request);

        ProblemDetails problem = (ProblemDetails) response.getBody();
        assertEquals(status, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertEquals(code, problem.code());
        assertEquals(detail, problem.detail());
    }

    private MockHttpServletRequest errorRequest(int status, String uri, String requestId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, uri);
        request.setAttribute(RequestIdFilter.ATTRIBUTE, requestId);
        return request;
    }
}

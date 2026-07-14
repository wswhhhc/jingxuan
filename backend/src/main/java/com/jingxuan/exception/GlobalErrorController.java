package com.jingxuan.exception;

import com.jingxuan.api.ApiPaths;
import com.jingxuan.api.ProblemDetails;
import com.jingxuan.api.RequestIdFilter;
import com.jingxuan.common.Result;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全局错误处理器 — 捕获 Spring Boot 默认错误（404 等），返回统一 JSON 格式。
 */
@RestController
public class GlobalErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<?> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode == null) {
            statusCode = 500;
        }
        String instance = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (instance == null) {
            instance = request.getRequestURI();
        }
        if (ApiPaths.isV1(instance)) {
            ErrorDescriptor descriptor = describe(statusCode);
            Object requestId = request.getAttribute(RequestIdFilter.ATTRIBUTE);
            ProblemDetails details = ProblemDetails.of(statusCode, descriptor.code(), descriptor.detail(), instance,
                    requestId == null ? request.getHeader(RequestIdFilter.HEADER) : requestId.toString());
            HttpStatus status = HttpStatus.resolve(statusCode);
            return ResponseEntity.status(status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(details);
        }
        if (statusCode == 404) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.fail(404, "请求的资源不存在"));
        }
        HttpStatus status = HttpStatus.resolve(statusCode);
        return ResponseEntity.status(status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Result.fail(statusCode, "服务器内部错误，请稍后重试"));
    }

    private ErrorDescriptor describe(int statusCode) {
        return switch (statusCode) {
            case 400 -> new ErrorDescriptor("BAD_REQUEST", "请求无效");
            case 401 -> new ErrorDescriptor("UNAUTHENTICATED", "未登录或登录已过期");
            case 403 -> new ErrorDescriptor("FORBIDDEN", "权限不足，无法访问");
            case 404 -> new ErrorDescriptor("NOT_FOUND", "请求的资源不存在");
            case 405 -> new ErrorDescriptor("METHOD_NOT_ALLOWED", "请求方法不支持");
            case 406 -> new ErrorDescriptor("NOT_ACCEPTABLE", "无法生成客户端可接受的响应格式");
            case 415 -> new ErrorDescriptor("UNSUPPORTED_MEDIA_TYPE", "请求媒体类型不受支持");
            case 429 -> new ErrorDescriptor("RATE_LIMITED", "访问过于频繁，请稍后再试");
            default -> new ErrorDescriptor("INTERNAL_ERROR", "服务器内部错误，请稍后重试");
        };
    }

    private record ErrorDescriptor(String code, String detail) {
    }
}

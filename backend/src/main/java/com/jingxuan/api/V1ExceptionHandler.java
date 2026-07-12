package com.jingxuan.api;

import com.jingxuan.exception.BusinessException;
import com.jingxuan.exception.NotFoundException;
import com.jingxuan.exception.UnauthorizedException;
import com.jingxuan.identityaccess.api.IdentityAccessProblemException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** v1 专用 RFC Problem Details 映射；旧接口继续由 GlobalExceptionHandler 处理。 */
@RestControllerAdvice(annotations = V1Api.class)
public class V1ExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetails> business(BusinessException exception, HttpServletRequest request) {
        int status = exception.getCode() >= 400 && exception.getCode() < 600 ? exception.getCode() : 400;
        return problem(status, "BUSINESS_ERROR", exception.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetails> unauthorized(UnauthorizedException exception, HttpServletRequest request) {
        int status = exception.isAuthFailed() ? 401 : 403;
        return problem(status, status == 401 ? "UNAUTHENTICATED" : "FORBIDDEN", exception.getMessage(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetails> notFound(NotFoundException exception, HttpServletRequest request) {
        return problem(404, "NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(IdentityAccessProblemException.class)
    public ResponseEntity<ProblemDetails> identityAccessProblem(IdentityAccessProblemException exception,
                                                                 HttpServletRequest request) {
        ProblemDetails details = ProblemDetails.of(exception.status(), exception.problemCode(), exception.getMessage(),
                request.getRequestURI(), requestId(request));
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(
                HttpStatus.resolve(exception.status()) != null
                        ? HttpStatus.resolve(exception.status())
                        : HttpStatus.INTERNAL_SERVER_ERROR);
        if (exception.retryAfterSeconds() != null) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.retryAfterSeconds()));
        }
        builder.header("Content-Type", "application/problem+json");
        return builder.body(details);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetails> accessDenied(HttpServletRequest request) {
        return problem(403, "FORBIDDEN", "权限不足，无法访问", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> validation(MethodArgumentNotValidException exception,
                                                     HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetails details = ProblemDetails.of(422, "VALIDATION_ERROR", "请求参数校验失败",
                request.getRequestURI(), requestId(request)).withFieldErrors(errors);
        return ResponseEntity.unprocessableEntity().body(details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> unexpected(Exception exception, HttpServletRequest request) {
        return problem(500, "INTERNAL_ERROR", "服务器内部错误，请稍后重试", request);
    }

    private ResponseEntity<ProblemDetails> problem(int status, String code, String detail,
                                                    HttpServletRequest request) {
        ProblemDetails details = ProblemDetails.of(status, code, detail,
                request.getRequestURI(), requestId(request));
        HttpStatus httpStatus = HttpStatus.resolve(status);
        return ResponseEntity.status(httpStatus == null ? HttpStatus.INTERNAL_SERVER_ERROR : httpStatus).body(details);
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.ATTRIBUTE);
        return value == null ? request.getHeader(RequestIdFilter.HEADER) : value.toString();
    }
}

package com.jingxuan.api;

import com.jingxuan.exception.BusinessException;
import com.jingxuan.exception.NotFoundException;
import com.jingxuan.exception.UnauthorizedException;
import com.jingxuan.identityaccess.api.IdentityAccessProblemException;
import com.jingxuan.identityaccess.api.RateLimitUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/** v1 专用 RFC Problem Details 映射；旧接口继续由 GlobalExceptionHandler 处理。 */
@RestControllerAdvice(annotations = V1Api.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
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
        return ResponseEntity.unprocessableContent()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetails> messageNotReadable(HttpServletRequest request) {
        return problem(400, "BAD_REQUEST", "请求体格式错误", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetails> missingRequestParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return problem(400, "BAD_REQUEST", "缺少必填参数: " + exception.getParameterName(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetails> argumentTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return problem(400, "BAD_REQUEST", "参数 " + exception.getName() + " 格式错误", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetails> mediaTypeNotSupported(HttpServletRequest request) {
        return problem(415, "UNSUPPORTED_MEDIA_TYPE", "请求媒体类型不受支持", request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemDetails> mediaTypeNotAcceptable(HttpServletRequest request) {
        return problem(406, "NOT_ACCEPTABLE", "无法生成客户端可接受的响应格式", request);
    }

    @ExceptionHandler(RateLimitUnavailableException.class)
    public ResponseEntity<ProblemDetails> rateLimitUnavailable(
            RateLimitUnavailableException exception, HttpServletRequest request) {
        return problem(503, "RATE_LIMIT_UNAVAILABLE", exception.getMessage(), request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetails> methodValidation(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            String parameterName = result.getMethodParameter().getParameterName();
            String field = parameterName == null ? "parameter" : parameterName;
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String message = error.getDefaultMessage();
                errors.putIfAbsent(field, message == null ? "参数值无效" : message);
            }
        }
        ProblemDetails details = ProblemDetails.of(422, "VALIDATION_ERROR", "请求参数校验失败",
                request.getRequestURI(), requestId(request)).withFieldErrors(errors);
        return ResponseEntity.unprocessableContent()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(details);
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
        return ResponseEntity.status(httpStatus == null ? HttpStatus.INTERNAL_SERVER_ERROR : httpStatus)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(details);
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.ATTRIBUTE);
        return value == null ? request.getHeader(RequestIdFilter.HEADER) : value.toString();
    }
}

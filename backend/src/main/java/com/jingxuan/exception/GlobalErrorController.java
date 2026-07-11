package com.jingxuan.exception;

import com.jingxuan.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全局错误处理器 — 捕获 Spring Boot 默认错误（404 等），返回统一 JSON 格式。
 */
@RestController
public class GlobalErrorController implements ErrorController {

    @RequestMapping("/error")
    public Result<Void> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        if (statusCode == null) {
            statusCode = 500;
        }
        if (statusCode == 404) {
            return Result.fail(404, "请求的资源不存在");
        }
        return Result.fail(statusCode, "服务器内部错误，请稍后重试");
    }
}

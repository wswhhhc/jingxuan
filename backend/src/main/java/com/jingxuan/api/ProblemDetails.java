package com.jingxuan.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/** RFC 9457 Problem Details 的平台扩展。 */
@Schema(name = "ProblemDetails", description = "统一 API 错误响应")
public record ProblemDetails(
        @Schema(description = "问题类型 URI") String type,
        @Schema(description = "问题标题") String title,
        @Schema(description = "HTTP 状态码") int status,
        @Schema(description = "面向用户的错误详情") String detail,
        @Schema(description = "请求实例 URI") String instance,
        @Schema(description = "机器可读业务错误码") String code,
        @Schema(description = "请求追踪 ID") String requestId,
        @Schema(description = "字段校验错误") Map<String, String> fieldErrors
) {

    public static ProblemDetails of(int status, String code, String detail,
                                    String instance, String requestId) {
        return new ProblemDetails(
                "https://api.jingxuan.local/problems/" + code.toLowerCase(),
                titleFor(status), status, detail, instance, code, requestId, Map.of());
    }

    public ProblemDetails withFieldErrors(Map<String, String> errors) {
        return new ProblemDetails(type, title, status, detail, instance, code, requestId,
                errors == null ? Map.of() : Map.copyOf(errors));
    }

    private static String titleFor(int status) {
        return switch (status) {
            case 400 -> "请求无效";
            case 401 -> "未认证";
            case 403 -> "禁止访问";
            case 404 -> "资源不存在";
            case 409 -> "请求冲突";
            case 422 -> "参数校验失败";
            default -> status >= 500 ? "服务器错误" : "请求失败";
        };
    }
}

package com.jingxuan.operationsreporting.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(name = "V1LogEntry", description = "操作日志条目")
public record V1LogEntry(
    @Schema(description = "日志 ID") String id,
    @Schema(description = "用户 ID") String userId,
    @Schema(description = "用户名") String username,
    @Schema(description = "操作行为") String action,
    @Schema(description = "操作目标类型") String target,
    @Schema(description = "操作目标 ID") String targetId,
    @Schema(description = "请求 IP") String ip,
    @Schema(description = "请求方法") String requestMethod,
    @Schema(description = "请求路径") String requestPath,
    @Schema(description = "是否成功") boolean success,
    @Schema(description = "错误信息") String errorMsg,
    @Schema(description = "耗时(ms)") Long duration,
    @Schema(description = "创建时间") OffsetDateTime createdAt
) {}

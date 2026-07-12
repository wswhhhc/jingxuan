package com.jingxuan.communication.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "V1NoticeRequest", description = "创建/更新公告请求")
public record V1NoticeRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank String content,
    boolean publishDirectly,
    String targetScope
) {
    public V1NoticeRequest {
        if (targetScope == null || targetScope.isBlank()) {
            targetScope = "all";
        }
    }
}

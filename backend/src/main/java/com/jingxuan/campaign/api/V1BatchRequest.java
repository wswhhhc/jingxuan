package com.jingxuan.campaign.api;

import jakarta.validation.constraints.NotBlank;

/** v1 创建/更新批次请求。 */
public record V1BatchRequest(
        @NotBlank String batchName,
        String batchType,
        String classScopes,
        String startTime,
        String endTime
) {
}

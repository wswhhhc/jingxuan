package com.jingxuan.evaluation.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** v1 创建/更新奖品请求。 */
@Schema(description = "创建/更新奖品请求")
public record V1PrizeRequest(
        @NotBlank String batchId,
        @NotBlank String rewardLevel,
        @NotBlank String rewardName,
        @NotBlank String prizeName,
        @NotNull @Positive Integer quota
) {}

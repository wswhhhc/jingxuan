package com.jingxuan.evaluation.api;

import io.swagger.v3.oas.annotations.media.Schema;

/** v1 奖品模型。 */
@Schema(description = "奖品")
public record V1Prize(
        String id,
        String batchId,
        String batchName,
        String rewardLevel,
        String rewardName,
        String prizeName,
        Integer quota
) {
}

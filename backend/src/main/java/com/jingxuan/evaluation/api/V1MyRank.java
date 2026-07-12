package com.jingxuan.evaluation.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** v1 我的排行。 */
@Schema(description = "我的排行")
public record V1MyRank(
        String batchId,
        String batchName,
        String workId,
        String workTitle,
        BigDecimal avgScore,
        BigDecimal avgInnovation,
        BigDecimal avgDifficulty,
        BigDecimal avgCompletion,
        BigDecimal avgPracticality,
        Integer teacherCount,
        Integer rankNo
) {
}

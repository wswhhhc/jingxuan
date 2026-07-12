package com.jingxuan.evaluation.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** v1 评分汇总。 */
@Schema(description = "评分汇总")
public record V1ScoreSummary(
        String workId,
        String workTitle,
        BigDecimal avgTotal,
        BigDecimal avgInnovation,
        BigDecimal avgDifficulty,
        BigDecimal avgCompletion,
        BigDecimal avgPracticality,
        Integer teacherCount
) {
}

package com.jingxuan.evaluation.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** v1 教师评分历史条目。 */
@Schema(description = "评分历史")
public record V1ScoreHistoryEntry(
        String id,
        String workId,
        String workTitle,
        String batchId,
        BigDecimal innovation,
        BigDecimal difficulty,
        BigDecimal completion,
        BigDecimal practicality,
        BigDecimal total,
        String comment,
        String scoredAt
) {
}

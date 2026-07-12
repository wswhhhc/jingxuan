package com.jingxuan.evaluation.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** v1 单条评分条目。 */
@Schema(description = "单条评分")
public record V1ScoreEntry(
        String id,
        String workId,
        String workTitle,
        String teacherId,
        String teacherName,
        BigDecimal innovation,
        BigDecimal difficulty,
        BigDecimal completion,
        BigDecimal practicality,
        BigDecimal total,
        String comment,
        String createdAt
) {
}

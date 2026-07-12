package com.jingxuan.evaluation.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** v1 批次作品评分明细（含各教师评分）。 */
@Schema(description = "批次作品评分明细")
public record V1BatchScoreDetail(
        String workId,
        String workTitle,
        String submitterName,
        java.util.List<V1TeacherScoreItem> scores
) {
    @Schema(description = "教师评分项")
    public record V1TeacherScoreItem(
            String teacherName,
            BigDecimal innovation,
            BigDecimal difficulty,
            BigDecimal completion,
            BigDecimal practicality,
            BigDecimal total,
            String comment
    ) {
    }
}

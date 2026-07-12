package com.jingxuan.evaluation.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** v1 排行榜条目。 */
@Schema(description = "排行榜条目")
public record V1LeaderboardEntry(
        Integer rankNo,
        String workId,
        String workTitle,
        String techStack,
        String coverUrl,
        String advisor,
        BigDecimal avgScore,
        BigDecimal avgInnovation,
        BigDecimal avgDifficulty,
        BigDecimal avgCompletion,
        BigDecimal avgPracticality,
        Integer teacherCount,
        String submitTime,
        String rewardLevel,
        String rewardName,
        String prizeName
) {
}

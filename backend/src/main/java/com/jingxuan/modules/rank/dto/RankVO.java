package com.jingxuan.modules.rank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "排行榜VO")
public class RankVO {

    @Schema(description = "排名")
    private Integer rankNo;

    @Schema(description = "作品ID")
    private Long workId;

    @Schema(description = "作品名称")
    private String workTitle;

    @Schema(description = "技术栈")
    private String techStack;

    @Schema(description = "封面图")
    private String coverUrl;

    @Schema(description = "指导教师")
    private String advisor;

    @Schema(description = "平均分")
    private BigDecimal avgScore;

    @Schema(description = "平均创新性")
    private BigDecimal avgInnovation;

    @Schema(description = "平均技术难度")
    private BigDecimal avgDifficulty;

    @Schema(description = "平均完成度")
    private BigDecimal avgCompletion;

    @Schema(description = "平均实用性")
    private BigDecimal avgPracticality;

    @Schema(description = "评分教师数")
    private Integer teacherCount;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "获奖等级文案，如「一等奖」")
    private String rewardLevel;

    @Schema(description = "兼容字段，与 rewardLevel 相同，保留用于向后兼容，新代码应使用 rewardLevel")
    private String rewardName;

    @Schema(description = "奖品说明，如「荣誉证书 + 500元京东卡」")
    private String prizeName;
}

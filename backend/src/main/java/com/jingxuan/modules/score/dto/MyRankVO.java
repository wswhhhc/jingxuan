package com.jingxuan.modules.score.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "学生个人评分排名VO")
public class MyRankVO {

    @Schema(description = "评分批次ID")
    private Long batchId;

    @Schema(description = "批次名称")
    private String batchName;

    @Schema(description = "作品ID")
    private Long workId;

    @Schema(description = "作品名称")
    private String workTitle;

    @Schema(description = "平均总分")
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

    @Schema(description = "排名")
    private Integer rankNo;
}

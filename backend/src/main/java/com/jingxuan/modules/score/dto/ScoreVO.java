package com.jingxuan.modules.score.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "评分VO")
public class ScoreVO {

    @Schema(description = "评分ID")
    private Long id;

    @Schema(description = "作品ID")
    private Long workId;

    @Schema(description = "作品名称")
    private String workTitle;

    @Schema(description = "教师ID")
    private Long teacherId;

    @Schema(description = "教师姓名")
    private String teacherName;

    @Schema(description = "创新性")
    private BigDecimal innovation;

    @Schema(description = "技术难度")
    private BigDecimal difficulty;

    @Schema(description = "完成度")
    private BigDecimal completion;

    @Schema(description = "实用性")
    private BigDecimal practicality;

    @Schema(description = "总分")
    private BigDecimal total;

    @Schema(description = "评语")
    private String comment;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

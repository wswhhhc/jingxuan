package com.jingxuan.modules.score.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "管理员评分明细VO")
public class AdminScoreDetailVO {

    @Schema(description = "作品ID")
    private Long workId;

    @Schema(description = "作品名称")
    private String workTitle;

    @Schema(description = "提交人姓名")
    private String submitterName;

    @Schema(description = "各教师评分明细")
    private List<TeacherScoreItem> scores;

    @Data
    @Schema(description = "教师评分项")
    public static class TeacherScoreItem {

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
    }
}

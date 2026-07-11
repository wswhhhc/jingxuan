package com.jingxuan.modules.score.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "评分提交请求")
public class ScoreSubmitRequest {

    @NotNull(message = "作品ID不能为空")
    @Schema(description = "作品ID")
    private Long workId;

    @NotNull(message = "创新性评分不能为空")
    @DecimalMin(value = "0.00", message = "创新性最低0分")
    @DecimalMax(value = "25.00", message = "创新性最高25分")
    @Schema(description = "创新性（满分25）")
    private BigDecimal innovation;

    @NotNull(message = "技术难度评分不能为空")
    @DecimalMin(value = "0.00", message = "技术难度最低0分")
    @DecimalMax(value = "25.00", message = "技术难度最高25分")
    @Schema(description = "技术难度（满分25）")
    private BigDecimal difficulty;

    @NotNull(message = "完成度评分不能为空")
    @DecimalMin(value = "0.00", message = "完成度最低0分")
    @DecimalMax(value = "30.00", message = "完成度最高30分")
    @Schema(description = "完成度（满分30）")
    private BigDecimal completion;

    @NotNull(message = "实用性评分不能为空")
    @DecimalMin(value = "0.00", message = "实用性最低0分")
    @DecimalMax(value = "20.00", message = "实用性最高20分")
    @Schema(description = "实用性（满分20）")
    private BigDecimal practicality;

    @Schema(description = "教师评语")
    private String comment;
}

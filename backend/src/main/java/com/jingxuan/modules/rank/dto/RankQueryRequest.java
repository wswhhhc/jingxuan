package com.jingxuan.modules.rank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "排行榜查询请求")
public class RankQueryRequest {

    @Schema(description = "评分批次ID，为空则默认查询最新批次")
    private Long batchId;

    @Schema(description = "技术栈筛选（分类排行）")
    private String techStack;

    @Schema(description = "前N名，默认10")
    private int topN = 10;
}

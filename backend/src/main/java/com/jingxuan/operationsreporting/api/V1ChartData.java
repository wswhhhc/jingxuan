package com.jingxuan.operationsreporting.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(name = "V1ChartData", description = "仪表盘图表数据")
public record V1ChartData(
    @Schema(description = "技术栈分布") List<Map<String, Object>> techStackDistribution,
    @Schema(description = "作品状态分布") Map<String, Long> statusDistribution,
    @Schema(description = "评分分布") Object scoreDistribution
) {}

package com.jingxuan.operationsreporting.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(name = "V1DashboardStats", description = "仪表盘统计")
public record V1DashboardStats(
    @Schema(description = "作品总数") long totalWorks,
    @Schema(description = "待审核数") long pendingAudit,
    @Schema(description = "已发布数") long publishedWorks,
    @Schema(description = "教师总数") long totalTeachers,
    @Schema(description = "学生总数") long totalStudents,
    @Schema(description = "活跃批次") long activeBatches,
    @Schema(description = "近期作品") List<Map<String, Object>> recentWorks,
    @Schema(description = "评分分布") Map<String, Object> scoreDistribution
) {}

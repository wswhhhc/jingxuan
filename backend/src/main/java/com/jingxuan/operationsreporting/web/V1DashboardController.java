package com.jingxuan.operationsreporting.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.operationsreporting.api.V1ChartData;
import com.jingxuan.operationsreporting.api.V1DashboardStats;
import com.jingxuan.operationsreporting.internal.application.DashboardQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@V1Api
@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "运营报表", description = "操作日志与仪表盘")
public class V1DashboardController {

    private final DashboardQueryService dashboardQueryService;

    public V1DashboardController(DashboardQueryService dashboardQueryService) {
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping("/stats")
    @Operation(summary = "仪表盘统计", description = "获取作品总数、待审核数、活跃批次等统计数据")
    public V1DashboardStats getStats() {
        return dashboardQueryService.getStats();
    }

    @GetMapping("/charts")
    @Operation(summary = "仪表盘图表数据", description = "获取技术栈分布、作品状态分布、评分分布等图表数据")
    public V1ChartData getCharts() {
        return dashboardQueryService.getChartData();
    }
}

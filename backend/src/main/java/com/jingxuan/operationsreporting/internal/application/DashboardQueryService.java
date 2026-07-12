package com.jingxuan.operationsreporting.internal.application;

import com.jingxuan.modules.adapter.AdminDashboardFacade;
import com.jingxuan.operationsreporting.api.V1ChartData;
import com.jingxuan.operationsreporting.api.V1DashboardStats;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DashboardQueryService {

    private final AdminDashboardFacade adminDashboardFacade;

    public DashboardQueryService(AdminDashboardFacade adminDashboardFacade) {
        this.adminDashboardFacade = adminDashboardFacade;
    }

    @SuppressWarnings("unchecked")
    public V1DashboardStats getStats() {
        Map<String, Object> stats = adminDashboardFacade.getStats();
        return new V1DashboardStats(
                toLong(stats.get("totalWorks")),
                toLong(stats.get("pendingAudit")),
                toLong(stats.get("publishedWorks")),
                toLong(stats.get("totalTeachers")),
                toLong(stats.get("totalStudents")),
                toLong(stats.get("activeBatches")),
                (List<Map<String, Object>>) stats.getOrDefault("recentWorks", List.of()),
                (Map<String, Object>) stats.getOrDefault("scoreDistribution", Map.of())
        );
    }

    public V1ChartData getChartData() {
        Map<String, Object> chart = adminDashboardFacade.getChartData();
        return new V1ChartData(
                extractList(chart, "techStackDist"),
                extractMap(chart, "statusDist"),
                chart.getOrDefault("scoreDist", List.of())
        );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof List ? (List<Map<String, Object>>) value : List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> extractMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, Long>) value : Map.of();
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }
}

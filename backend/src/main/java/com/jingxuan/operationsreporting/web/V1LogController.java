package com.jingxuan.operationsreporting.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.api.V1Page;
import com.jingxuan.operationsreporting.api.V1LogEntry;
import com.jingxuan.operationsreporting.internal.application.LogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@V1Api
@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "运营报表", description = "操作日志与仪表盘")
public class V1LogController {

    private final LogQueryService logQueryService;

    public V1LogController(LogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    @GetMapping
    @Operation(summary = "操作日志列表", description = "分页查询操作日志，支持按操作类型和用户 ID 筛选")
    public V1Page<V1LogEntry> listLogs(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页条数") int size,
            @RequestParam(required = false) @Parameter(description = "操作类型筛选") String action,
            @RequestParam(required = false) @Parameter(description = "用户 ID") String userId) {

        Long parsedUserId = userId != null ? V1Ids.parse(userId, "userId") : null;
        return logQueryService.queryLogs(page, size, action, parsedUserId);
    }
}

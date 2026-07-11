package com.jingxuan.campaign.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.campaign.api.V1Batch;
import com.jingxuan.campaign.api.V1Task;
import com.jingxuan.campaign.internal.application.CampaignQueryService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** v1 批次与学生待办读取入口。 */
@V1Api
@RestController
@RequestMapping("/api/v1")
@Tag(name = "v1 批次与待办", description = "可参与批次和我的待办")
public class V1CampaignController {
    private final CampaignQueryService queryService;
    public V1CampaignController(CampaignQueryService queryService) { this.queryService = queryService; }

    @GetMapping("/batches")
    @Operation(summary = "获取当前用户可参与批次")
    public ResponseEntity<List<V1Batch>> batches() {
        return ResponseEntity.ok(queryService.availableBatches(SecurityUtils.requireCurrentUserId()));
    }

    @GetMapping("/me/tasks")
    @Operation(summary = "获取我的待办")
    public ResponseEntity<List<V1Task>> tasks() {
        return ResponseEntity.ok(queryService.myTasks(SecurityUtils.requireCurrentUserId()));
    }
}

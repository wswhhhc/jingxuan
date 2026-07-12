package com.jingxuan.campaign.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.campaign.api.V1Batch;
import com.jingxuan.campaign.api.V1Task;
import com.jingxuan.campaign.api.V1CompleteTaskRequest;
import com.jingxuan.campaign.internal.application.CampaignCommandService;
import com.jingxuan.campaign.internal.application.CampaignQueryService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

import java.util.List;

/** v1 批次与学生待办读取入口。 */
@V1Api
@RestController
@RequestMapping("/api/v1")
@Tag(name = "v1 批次与待办", description = "可参与批次和我的待办")
public class V1CampaignController {
    private final CampaignQueryService queryService;
    private final CampaignCommandService commandService;
    public V1CampaignController(CampaignQueryService queryService, CampaignCommandService commandService) {
        this.queryService = queryService; this.commandService = commandService;
    }

    @GetMapping("/me/batches")
    @Operation(summary = "获取当前用户可参与批次")
    public ResponseEntity<List<V1Batch>> myBatches() {
        return ResponseEntity.ok(queryService.availableBatches(SecurityUtils.requireCurrentUserId()));
    }

    @GetMapping("/me/tasks")
    @Operation(summary = "获取我的待办")
    public ResponseEntity<List<V1Task>> tasks() {
        return ResponseEntity.ok(queryService.myTasks(SecurityUtils.requireCurrentUserId()));
    }

    @PostMapping("/me/tasks/{taskId}/completion")
    @Operation(summary = "提交作品后完成我的待办")
    public ResponseEntity<Void> completeTask(@PathVariable Long taskId,
                                             @Valid @RequestBody V1CompleteTaskRequest request) {
        commandService.completeTask(SecurityUtils.requireCurrentUserId(), taskId, Long.valueOf(request.workId()));
        return ResponseEntity.noContent().build();
    }
}

package com.jingxuan.campaign.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.campaign.api.V1BatchDetail;
import com.jingxuan.campaign.api.V1BatchPage;
import com.jingxuan.campaign.api.V1BatchRequest;
import com.jingxuan.campaign.api.V1NoticeRequest;
import com.jingxuan.campaign.internal.application.CampaignAdminCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** v1 管理端批次管理入口。 */
@V1Api
@RestController
@RequestMapping("/api/v1/batches")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "v1 管理端批次管理", description = "管理员管理评分批次、待办与排行榜")
public class V1CampaignAdminController {
    private final CampaignAdminCommandService adminCommandService;

    public V1CampaignAdminController(CampaignAdminCommandService adminCommandService) {
        this.adminCommandService = adminCommandService;
    }

    @GetMapping
    @Operation(summary = "获取批次列表（分页）")
    public ResponseEntity<V1BatchPage> listBatches(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(adminCommandService.listBatches(page, pageSize));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建批次")
    public V1BatchDetail createBatch(@Valid @RequestBody V1BatchRequest request) {
        return adminCommandService.createBatch(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取批次详情")
    public ResponseEntity<V1BatchDetail> getBatch(@PathVariable String id) {
        return ResponseEntity.ok(adminCommandService.getBatch(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新批次")
    public ResponseEntity<V1BatchDetail> updateBatch(@PathVariable String id,
                                                      @Valid @RequestBody V1BatchRequest request) {
        return ResponseEntity.ok(adminCommandService.updateBatch(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除批次")
    public void deleteBatch(@PathVariable String id) {
        adminCommandService.deleteBatch(id);
    }

    @PutMapping("/{id}/notice")
    @Operation(summary = "保存待办要求")
    public void saveNotice(@PathVariable String id,
                           @Valid @RequestBody V1NoticeRequest request) {
        adminCommandService.saveNotice(id, request.title(), request.content());
    }

    @PostMapping("/{id}/tasks/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "发布待办")
    public void publishTasks(@PathVariable String id) {
        adminCommandService.publishTasks(id);
    }

    @PostMapping("/{id}/ranking/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "公示排行榜")
    public void publishRanking(@PathVariable String id) {
        adminCommandService.publishRanking(id);
    }

    @PostMapping("/{id}/ranking/unpublish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "取消公示排行榜")
    public void unpublishRanking(@PathVariable String id) {
        adminCommandService.unpublishRanking(id);
    }
}

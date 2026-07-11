package com.jingxuan.modules.scorebatch.controller;

import com.jingxuan.common.Result;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.modules.scorebatch.service.ScoreBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评分批次管理接口
 */
@Tag(name = "评分批次管理", description = "创建、更新、查询评分批次")
@RestController
@RequestMapping("/score-batch")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ScoreBatchController {

    private final ScoreBatchService scoreBatchService;

    @Operation(summary = "创建评分批次")
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody ScoreBatch entity) {
        Long id = scoreBatchService.createBatch(entity);
        return Result.ok(id);
    }

    @Operation(summary = "更新评分批次")
    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody ScoreBatch entity) {
        scoreBatchService.updateBatch(entity);
        return Result.ok();
    }

    @Operation(summary = "查询评分批次列表")
    @PostMapping("/list")
    public Result<Object> list(@RequestParam(defaultValue = "1") int pageNum,
                               @RequestParam(defaultValue = "10") int pageSize) {
        Object pageResult = scoreBatchService.queryBatchList(pageNum, pageSize);
        return Result.ok(pageResult);
    }

    @Operation(summary = "公示排行榜")
    @PostMapping("/{id}/publish-ranking")
    public Result<Void> publishRanking(@PathVariable Long id) {
        scoreBatchService.publishRanking(id);
        return Result.ok();
    }

    @Operation(summary = "取消排行榜公示")
    @PostMapping("/{id}/unpublish-ranking")
    public Result<Void> unpublishRanking(@PathVariable Long id) {
        scoreBatchService.unpublishRanking(id);
        return Result.ok();
    }

    @Operation(summary = "删除评分批次")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scoreBatchService.deleteBatch(id);
        return Result.ok();
    }

    @Operation(summary = "获取当前活跃批次")
    @GetMapping("/active")
    public Result<Object> getActive() {
        Object batch = scoreBatchService.getActiveBatch();
        return Result.ok(batch);
    }

    @Operation(summary = "保存批次待办要求内容")
    @PutMapping("/{id}/notice")
    public Result<Void> saveNotice(@PathVariable Long id, @RequestBody Map<String, String> body) {
        scoreBatchService.saveNotice(id, body.get("noticeTitle"), body.get("noticeContent"));
        return Result.ok();
    }

    @Operation(summary = "发布批次待办（给班级范围内所有学生创建待办任务）")
    @PostMapping("/{id}/publish-task")
    public Result<Void> publishTask(@PathVariable Long id) {
        scoreBatchService.publishTask(id);
        return Result.ok();
    }

    /**
     * 兼容旧端点，底层逻辑已替换为发布待办
     */
    @Operation(summary = "发布批次通知（已迁移至待办，保留兼容）")
    @PostMapping("/{id}/publish-notice")
    public Result<Void> publishNotice(@PathVariable Long id) {
        scoreBatchService.publishTask(id);
        return Result.ok();
    }
}

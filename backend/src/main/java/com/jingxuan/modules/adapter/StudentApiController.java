package com.jingxuan.modules.adapter;

import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.StudentTask;
import com.jingxuan.modules.deleterequest.service.DeleteRequestService;
import com.jingxuan.modules.score.dto.MyRankVO;
import com.jingxuan.modules.scorebatch.service.ScoreBatchService;
import com.jingxuan.modules.task.service.StudentTaskService;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkListVO;
import com.jingxuan.modules.work.dto.WorkQueryRequest;
import com.jingxuan.modules.work.dto.WorkRequest;
import com.jingxuan.modules.work.service.WorkService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生端 API 适配 — 桥接前端期望的学生端路径与后端实际接口
 */
@Tag(name = "学生端API适配")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentApiController {

    private final WorkService workService;
    private final StudentRankingFacade studentRankingFacade;
    private final ScoreBatchService scoreBatchService;
    private final StudentTaskService studentTaskService;
    private final DeleteRequestService deleteRequestService;

    @Operation(summary = "创建作品")
    @PostMapping("/student/works")
    public Result<Long> createWork(@Valid @RequestBody WorkRequest request) {
        Long workId = workService.createWork(request);
        return Result.ok(workId);
    }

    @Operation(summary = "获取我的作品列表")
    @GetMapping("/student/works")
    public Result<PageResult<WorkListVO>> getMyWorks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Long userId = SecurityUtils.requireCurrentUserId();
        WorkQueryRequest query = new WorkQueryRequest();
        query.setPageNum(page);
        query.setPageSize(size);
        query.setParticipantUserId(userId);
        query.setStatus(status);
        return Result.ok(workService.queryWorkList(query));
    }

    @Operation(summary = "获取作品详情")
    @GetMapping("/student/works/{id}")
    public Result<WorkDetailVO> getWorkDetail(@PathVariable Long id) {
        WorkDetailVO vo = workService.getCurrentStudentWorkDetail(id);
        return Result.ok(vo);
    }

    @Operation(summary = "更新作品")
    @PutMapping("/student/works/{id}")
    public Result<Void> updateWork(@PathVariable Long id,
                                   @Valid @RequestBody WorkRequest request) {
        workService.updateWork(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除作品")
    @DeleteMapping("/student/works/{id}")
    public Result<Void> deleteWork(@PathVariable Long id) {
        workService.deleteWork(id);
        return Result.ok();
    }

    @Operation(summary = "提交作品审核")
    @PostMapping("/student/works/{id}/submit")
    public Result<Void> submitWork(@PathVariable Long id) {
        workService.submitWork(id);
        return Result.ok();
    }

    @Operation(summary = "获取当前学生可参与的评分批次列表")
    @GetMapping("/student/batch/available")
    public Result<List<ScoreBatch>> getAvailableBatches() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.ok(scoreBatchService.getAvailableBatchesForStudent(userId));
    }

    @Operation(summary = "获取我的评分与排名（仅排行榜已公示时返回）")
    @GetMapping("/student/score/my-ranks")
    public Result<List<MyRankVO>> getMyRanks() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.ok(studentRankingFacade.getPublishedRanks(userId));
    }

    @Operation(summary = "获取我的待办列表")
    @GetMapping("/student/tasks")
    public Result<List<StudentTask>> getMyTasks() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.ok(studentTaskService.getStudentTasks(userId));
    }

    @Operation(summary = "提交作品后标记待办为已完成")
    @PostMapping("/student/tasks/{taskId}/complete")
    public Result<Void> completeTask(@PathVariable Long taskId,
                                     @RequestParam Long workId) {
        studentTaskService.completeTask(taskId, workId);
        return Result.ok();
    }

    @Operation(summary = "申请删除已审核通过的作品")
    @PostMapping("/student/work/{workId}/delete-request")
    public Result<Long> submitDeleteRequest(@PathVariable Long workId,
                                            @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.requireCurrentUserId();
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            return Result.fail("请填写申请原因");
        }
        Long id = deleteRequestService.submitRequest(workId, userId, reason);
        return Result.ok(id);
    }
}

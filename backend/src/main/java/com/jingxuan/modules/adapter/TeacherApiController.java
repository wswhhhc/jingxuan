package com.jingxuan.modules.adapter;

import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.modules.rank.dto.RankQueryRequest;
import com.jingxuan.modules.rank.dto.RankVO;
import com.jingxuan.modules.rank.service.RankService;
import com.jingxuan.modules.score.dto.ScoreSubmitRequest;
import com.jingxuan.modules.score.dto.ScoreVO;
import com.jingxuan.modules.score.dto.TeacherScoreHistoryVO;
import com.jingxuan.modules.score.service.ScoreService;
import com.jingxuan.modules.scorebatch.service.ScoreBatchService;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkListVO;
import com.jingxuan.security.SecurityUtils;

import java.util.Collections;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "教师端 API")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherApiController {

    private final TeacherWorkFacade teacherWorkFacade;
    private final ScoreService scoreService;
    private final ScoreBatchService scoreBatchService;
    private final RankService rankService;

    @GetMapping("/teacher/work/list")
    @Operation(summary = "查询作品列表（教师端，仅展示已审核通过的作品）")
    public Result<PageResult<WorkListVO>> listWorks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String techStack,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Boolean onlyUnscored) {
        Long teacherId = SecurityUtils.requireCurrentUserId();
        return Result.ok(teacherWorkFacade.queryScoredWorks(page, size, keyword, techStack,
                batchId, onlyUnscored, teacherId));
    }

    @GetMapping("/teacher/work/{id}")
    @Operation(summary = "获取作品详情（匿名评审，隐藏学生姓名和成员信息）")
    public Result<WorkDetailVO> getWorkDetail(@PathVariable Long id) {
        return Result.ok(teacherWorkFacade.getAnonymousApprovedWorkDetail(id));
    }

    @PostMapping("/teacher/score")
    @Operation(summary = "提交评分")
    public Result<Void> submitScore(@Valid @RequestBody ScoreSubmitRequest request) {
        Long teacherId = SecurityUtils.requireCurrentUserId();
        scoreService.submitScore(teacherId, request);
        return Result.ok();
    }

    @GetMapping("/teacher/score/{workId}")
    @Operation(summary = "获取教师对某作品的评分")
    public Result<ScoreVO> getTeacherScore(@PathVariable Long workId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.ok(scoreService.getTeacherScore(workId, userId));
    }

    @GetMapping("/teacher/score/history")
    @Operation(summary = "获取评分历史")
    public Result<PageResult<TeacherScoreHistoryVO>> getScoreHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long teacherId = SecurityUtils.requireCurrentUserId();
        return Result.ok(teacherWorkFacade.queryScoreHistory(teacherId, page, size));
    }

    @GetMapping("/teacher/dashboard/stats")
    @Operation(summary = "获取教师工作台统计")
    public Result<Map<String, Object>> getDashboardStats() {
        Long teacherId = SecurityUtils.requireCurrentUserId();
        return Result.ok(teacherWorkFacade.getDashboardStats(teacherId));
    }

    @GetMapping("/teacher/batch/list")
    @Operation(summary = "获取评分批次列表")
    public Result<List<ScoreBatch>> listBatches() {
        return Result.ok(scoreBatchService.list());
    }

    @GetMapping("/teacher/ranking/list")
    @Operation(summary = "获取排行榜（必须指定批次，且仅已公示批次可见）")
    public Result<List<RankVO>> listRanking(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false, defaultValue = "10") int topN,
            @RequestParam(required = false) String type) {
        if (batchId == null) {
            return Result.ok(Collections.emptyList());
        }
        if (!scoreBatchService.isRankPublished(batchId)) {
            return Result.ok(Collections.emptyList());
        }
        RankQueryRequest request = new RankQueryRequest();
        request.setBatchId(batchId);
        request.setTopN(topN);
        request.setTechStack(type);
        return Result.ok(rankService.getRankList(request));
    }

    @GetMapping("/teacher/ranking/batches")
    @Operation(summary = "获取排行榜批次列表（仅返回已公示的批次）")
    public Result<List<ScoreBatch>> listRankingBatches() {
        List<ScoreBatch> all = scoreBatchService.list();
        List<ScoreBatch> published = all.stream()
                .filter(b -> Integer.valueOf(1).equals(b.getRankPublished()))
                .collect(Collectors.toList());
        return Result.ok(published);
    }

    @GetMapping("/teacher/ranking/categories")
    @Operation(summary = "获取排行分类（技术栈，仅已公示批次的分类可见）")
    public Result<List<Map<String, String>>> listRankingCategories(
            @RequestParam(required = false) Long batchId) {
        return Result.ok(teacherWorkFacade.listRankingCategories(batchId));
    }

    @PostMapping("/teacher/ranking/refresh")
    @Operation(summary = "刷新排行榜缓存")
    public Result<Void> refreshRanking(@RequestParam(required = false) Long batchId) {
        rankService.refreshRankCache(batchId);
        return Result.ok();
    }

    @PostMapping("/teacher/ranking/refresh/{batchId}")
    @Operation(summary = "刷新排行榜缓存（按批次）")
    public Result<Void> refreshRankingByBatch(@PathVariable Long batchId) {
        rankService.refreshRankCache(batchId);
        return Result.ok();
    }
}

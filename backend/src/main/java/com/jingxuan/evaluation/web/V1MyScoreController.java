package com.jingxuan.evaluation.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Page;
import com.jingxuan.evaluation.api.V1ScoreHistoryEntry;
import com.jingxuan.evaluation.internal.application.ScoreQueryService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** v1 教师个人评分历史端点。 */
@V1Api
@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "v1 我的评分历史")
public class V1MyScoreController {

    private final ScoreQueryService scoreQueryService;

    public V1MyScoreController(ScoreQueryService scoreQueryService) {
        this.scoreQueryService = scoreQueryService;
    }

    @GetMapping("/scores/history")
    @Operation(summary = "获取我的评分历史（分页）")
    public ResponseEntity<V1Page<V1ScoreHistoryEntry>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                scoreQueryService.getMyScoreHistory(SecurityUtils.requireCurrentUserId(), page, size));
    }
}

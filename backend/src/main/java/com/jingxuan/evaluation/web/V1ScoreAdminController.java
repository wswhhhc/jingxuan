package com.jingxuan.evaluation.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.evaluation.api.V1BatchScoreDetail;
import com.jingxuan.evaluation.internal.application.ScoreQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** v1 管理端评分明细端点。 */
@V1Api
@RestController
@RequestMapping("/api/v1/scores")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "v1 管理端评分明细")
public class V1ScoreAdminController {

    private final ScoreQueryService scoreQueryService;

    public V1ScoreAdminController(ScoreQueryService scoreQueryService) {
        this.scoreQueryService = scoreQueryService;
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "获取批次评分明细（含各教师评分）")
    public ResponseEntity<List<V1BatchScoreDetail>> batchScores(@PathVariable String batchId) {
        return ResponseEntity.ok(
                scoreQueryService.getBatchScoreDetail(V1Ids.parse(batchId, "batchId")));
    }
}

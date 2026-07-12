package com.jingxuan.evaluation.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.evaluation.api.V1LeaderboardEntry;
import com.jingxuan.evaluation.api.V1MyRank;
import com.jingxuan.evaluation.internal.application.LeaderboardCommandService;
import com.jingxuan.evaluation.internal.application.LeaderboardQueryService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** v1 排行榜端点。 */
@V1Api
@RestController
@RequestMapping("/api/v1/leaderboards")
@Tag(name = "v1 排行榜")
public class V1LeaderboardController {

    private final LeaderboardQueryService leaderboardQueryService;
    private final LeaderboardCommandService leaderboardCommandService;

    public V1LeaderboardController(LeaderboardQueryService leaderboardQueryService,
                                   LeaderboardCommandService leaderboardCommandService) {
        this.leaderboardQueryService = leaderboardQueryService;
        this.leaderboardCommandService = leaderboardCommandService;
    }

    @GetMapping
    @Operation(summary = "获取排行榜", description = "必须指定批次，且仅已公示批次可见。无需认证。")
    public ResponseEntity<List<V1LeaderboardEntry>> listRanking(
            @RequestParam Long batchId,
            @RequestParam(defaultValue = "10") int topN,
            @RequestParam(required = false) String type) {
        if (!leaderboardQueryService.isRankPublished(batchId)) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(leaderboardQueryService.getCategoryRank(type, batchId, topN));
        }
        return ResponseEntity.ok(leaderboardQueryService.getRankList(batchId, topN, null));
    }

    @GetMapping("/categories")
    @Operation(summary = "获取排行分类（技术栈）")
    public ResponseEntity<List<Map<String, String>>> categories(
            @RequestParam(required = false) Long batchId) {
        return ResponseEntity.ok(leaderboardQueryService.listRankingCategories(batchId));
    }

    @PostMapping("/cache/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "刷新排行榜缓存")
    public void refreshCache(@RequestParam(required = false) Long batchId) {
        leaderboardCommandService.refreshCache(batchId);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "获取我的排行", description = "返回学生参与的所有已公示批次的排名")
    public ResponseEntity<List<V1MyRank>> myRanks() {
        return ResponseEntity.ok(leaderboardQueryService.getMyRanks(SecurityUtils.requireCurrentUserId()));
    }
}

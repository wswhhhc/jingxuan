package com.jingxuan.evaluation.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.api.V1Page;
import com.jingxuan.evaluation.api.V1IssueRequest;
import com.jingxuan.evaluation.api.V1LeaderboardEntry;
import com.jingxuan.evaluation.api.V1Prize;
import com.jingxuan.evaluation.api.V1PrizeRequest;
import com.jingxuan.evaluation.api.V1RewardIssue;
import com.jingxuan.evaluation.internal.application.PrizeCommandService;
import com.jingxuan.evaluation.internal.application.PrizeQueryService;
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

import java.util.List;

/** v1 管理端奖品管理端点。 */
@V1Api
@RestController
@RequestMapping("/api/v1/prizes")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "v1 管理端奖品管理", description = "管理员管理奖品配置与发放")
public class V1PrizeController {

    private final PrizeQueryService prizeQueryService;
    private final PrizeCommandService prizeCommandService;

    public V1PrizeController(PrizeQueryService prizeQueryService, PrizeCommandService prizeCommandService) {
        this.prizeQueryService = prizeQueryService;
        this.prizeCommandService = prizeCommandService;
    }

    @GetMapping
    @Operation(summary = "获取奖品列表（分页）")
    public ResponseEntity<V1Page<V1Prize>> listPrizes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long batchId) {
        return ResponseEntity.ok(prizeQueryService.listPrizes(page, size, batchId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建奖品")
    public V1Prize createPrize(@Valid @RequestBody V1PrizeRequest request) {
        return prizeCommandService.createPrize(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新奖品")
    public ResponseEntity<Void> updatePrize(@PathVariable String id,
                                             @Valid @RequestBody V1PrizeRequest request) {
        prizeCommandService.updatePrize(V1Ids.parse(id, "id"), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除奖品")
    public void deletePrize(@PathVariable String id) {
        prizeCommandService.deletePrize(V1Ids.parse(id, "id"));
    }

    @GetMapping("/{id}/issues")
    @Operation(summary = "获取奖品发放记录（分页）")
    public ResponseEntity<V1Page<V1RewardIssue>> listIssues(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                prizeQueryService.listIssues(page, size, V1Ids.parse(id, "id")));
    }

    @PostMapping("/{id}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "发放奖品")
    public void issuePrize(@PathVariable String id, @Valid @RequestBody V1IssueRequest request) {
        prizeCommandService.issuePrize(V1Ids.parse(id, "id"), Long.valueOf(request.workId()));
    }

    @PutMapping("/issues/{issueId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "取消奖品发放")
    public void cancelIssue(@PathVariable String issueId) {
        prizeCommandService.cancelIssue(V1Ids.parse(issueId, "issueId"));
    }

    @GetMapping("/ranked-works")
    @Operation(summary = "获取排行作品列表（用于发奖时选择）")
    public ResponseEntity<List<V1LeaderboardEntry>> rankedWorks(
            @RequestParam Long batchId,
            @RequestParam(defaultValue = "50") int topN) {
        return ResponseEntity.ok(prizeQueryService.getRankedWorks(batchId, topN));
    }
}

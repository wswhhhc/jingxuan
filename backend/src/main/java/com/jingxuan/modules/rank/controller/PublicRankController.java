package com.jingxuan.modules.rank.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.common.Result;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.rank.dto.RankQueryRequest;
import com.jingxuan.modules.rank.dto.RankVO;
import com.jingxuan.modules.rank.service.RankService;
import com.jingxuan.modules.scorebatch.service.ScoreBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Tag(name = "公共排行榜 API")
@RestController
@RequiredArgsConstructor
public class PublicRankController {

    private final RankService rankService;
    private final ScoreBatchMapper scoreBatchMapper;
    private final WorkMapper workMapper;
    private final ScoreBatchService scoreBatchService;

    @Operation(summary = "获取公共排行榜（仅已公示批次可见）")
    @GetMapping("/public/ranking/list")
    public Result<List<RankVO>> getRankingList(
            @RequestParam(required = false) Long batchId,
            @RequestParam(defaultValue = "10") int topN,
            @RequestParam(required = false) String techStack) {
        // 如果未指定批次，尝试获取最新批次并检查是否已公示
        Long resolvedBatchId = batchId;
        if (resolvedBatchId == null) {
            ScoreBatch latest = scoreBatchMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScoreBatch>()
                            .eq(ScoreBatch::getDeleted, 0)
                            .orderByDesc(ScoreBatch::getCreateTime)
                            .last("LIMIT 1"));
            if (latest != null) {
                resolvedBatchId = latest.getId();
            }
        }
        if (resolvedBatchId != null && !scoreBatchService.isRankPublished(resolvedBatchId)) {
            return Result.ok(Collections.emptyList());
        }
        RankQueryRequest request = new RankQueryRequest();
        request.setBatchId(resolvedBatchId);
        request.setTopN(topN);
        request.setTechStack(techStack);
        return Result.ok(rankService.getRankList(request));
    }

    @Operation(summary = "获取已公示排行榜的评分批次列表")
    @GetMapping("/public/ranking/batches")
    public Result<List<Map<String, Object>>> getRankingBatches() {
        List<ScoreBatch> allBatches = scoreBatchMapper.selectList(
                Wrappers.<ScoreBatch>lambdaQuery()
                        .eq(ScoreBatch::getDeleted, 0)
                        .eq(ScoreBatch::getRankPublished, 1)
                        .orderByDesc(ScoreBatch::getCreateTime));
        List<Map<String, Object>> result = allBatches.stream()
                .filter(b -> hasRankingData(b.getId()))
                .map(b -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("batchId", b.getId());
                    item.put("batchName", b.getBatchName());
                    item.put("startTime", b.getStartTime());
                    item.put("endTime", b.getEndTime());
                    return item;
                })
                .collect(Collectors.toList());
        return Result.ok(result);
    }

    @Operation(summary = "获取排行榜涉及的技术栈分类（仅已公示批次可见）")
    @GetMapping("/public/ranking/categories")
    public Result<List<String>> getRankingCategories(
            @RequestParam(required = false) Long batchId) {
        if (batchId != null && !scoreBatchService.isRankPublished(batchId)) {
            return Result.ok(Collections.emptyList());
        }
        List<String> techStacks = workMapper.selectDistinctTechStacksFromRanked(batchId).stream()
                .filter(Objects::nonNull)
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        return Result.ok(techStacks);
    }

    /**
     * 检查指定批次是否有排行数据（有已评分的作品）
     */
    private boolean hasRankingData(Long batchId) {
        Long count = workMapper.countRankedWorksByBatch(batchId);
        return count != null && count > 0;
    }
}

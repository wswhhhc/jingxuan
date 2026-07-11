package com.jingxuan.modules.adapter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.Work;
import com.jingxuan.enums.AuditStatusEnum;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.modules.rank.dto.RankQueryRequest;
import com.jingxuan.modules.rank.dto.RankVO;
import com.jingxuan.modules.rank.service.RankService;
import com.jingxuan.modules.score.dto.MyRankVO;
import com.jingxuan.modules.score.dto.ScoreSummaryVO;
import com.jingxuan.modules.score.service.ScoreService;
import com.jingxuan.modules.work.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentRankingFacade {

    private final WorkService workService;
    private final ScoreBatchMapper scoreBatchMapper;
    private final ScoreService scoreService;
    private final RankService rankService;

    public List<MyRankVO> getPublishedRanks(Long userId) {
        List<Work> myWorks = workService.listParticipatedWorks(userId).stream()
                .filter(work -> Integer.valueOf(AuditStatusEnum.APPROVED.getValue()).equals(work.getStatus()))
                .filter(work -> work.getBatchId() != null)
                .toList();

        if (myWorks.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ScoreBatch> batchMap = getPublishedBatchMap(myWorks);
        if (batchMap.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<RankVO>> rankCache = loadRankCache(batchMap.keySet());
        List<MyRankVO> result = new ArrayList<>();
        for (Work work : myWorks) {
            MyRankVO rank = toMyRankVO(work, batchMap.get(work.getBatchId()), rankCache.get(work.getBatchId()));
            if (rank != null) {
                result.add(rank);
            }
        }
        return result;
    }

    private Map<Long, ScoreBatch> getPublishedBatchMap(List<Work> works) {
        Set<Long> batchIds = works.stream()
                .map(Work::getBatchId)
                .collect(Collectors.toSet());
        // 只返回 rank_published = 1 的已公示批次，未公示的排名对学生不可见
        List<ScoreBatch> publishedBatches = scoreBatchMapper.selectList(
                Wrappers.<ScoreBatch>lambdaQuery()
                        .in(ScoreBatch::getId, batchIds)
                        .eq(ScoreBatch::getRankPublished, 1));
        return publishedBatches.stream()
                .collect(Collectors.toMap(ScoreBatch::getId, batch -> batch));
    }

    private Map<Long, List<RankVO>> loadRankCache(Set<Long> batchIds) {
        Map<Long, List<RankVO>> rankCache = new HashMap<>();
        for (Long batchId : batchIds) {
            RankQueryRequest rankQuery = new RankQueryRequest();
            rankQuery.setBatchId(batchId);
            rankQuery.setTopN(Integer.MAX_VALUE);
            rankCache.put(batchId, rankService.getRankList(rankQuery));
        }
        return rankCache;
    }

    private MyRankVO toMyRankVO(Work work, ScoreBatch batch, List<RankVO> fullRank) {
        if (batch == null || fullRank == null || fullRank.isEmpty()) {
            return null;
        }
        ScoreSummaryVO summary = scoreService.getScoreSummary(work.getId());
        if (summary == null) {
            return null;
        }

        MyRankVO vo = new MyRankVO();
        vo.setBatchId(batch.getId());
        vo.setBatchName(batch.getBatchName());
        vo.setWorkId(work.getId());
        vo.setWorkTitle(work.getTitle());
        vo.setAvgScore(summary.getAvgTotal());
        vo.setAvgInnovation(summary.getAvgInnovation());
        vo.setAvgDifficulty(summary.getAvgDifficulty());
        vo.setAvgCompletion(summary.getAvgCompletion());
        vo.setAvgPracticality(summary.getAvgPracticality());
        vo.setTeacherCount(summary.getTeacherCount());
        fullRank.stream()
                .filter(rank -> rank.getWorkId().equals(work.getId()))
                .findFirst()
                .ifPresent(rank -> vo.setRankNo(rank.getRankNo()));
        return vo;
    }
}

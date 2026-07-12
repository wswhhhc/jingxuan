package com.jingxuan.evaluation.internal.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.api.V1Page;
import com.jingxuan.api.V1PageInfo;
import com.jingxuan.entity.RewardIssue;
import com.jingxuan.evaluation.api.V1Prize;
import com.jingxuan.evaluation.api.V1RewardIssue;
import com.jingxuan.modules.rank.dto.RankQueryRequest;
import com.jingxuan.modules.rank.dto.RankVO;
import com.jingxuan.modules.rank.service.RankService;
import com.jingxuan.modules.prize.service.PrizeService;
import com.jingxuan.modules.prize.service.RewardIssueService;
import com.jingxuan.evaluation.api.V1LeaderboardEntry;
import org.springframework.stereotype.Service;

import java.util.List;

/** 奖品查询用例。 */
@Service
public class PrizeQueryService {

    private final PrizeService prizeService;
    private final RewardIssueService rewardIssueService;
    private final RankService rankService;

    public PrizeQueryService(PrizeService prizeService, RewardIssueService rewardIssueService,
                             RankService rankService) {
        this.prizeService = prizeService;
        this.rewardIssueService = rewardIssueService;
        this.rankService = rankService;
    }

    /** 获取奖品列表（分页）。 */
    public V1Page<V1Prize> listPrizes(int page, int size, Long batchId) {
        var mpPage = prizeService.queryPrizeList(page, size, batchId);
        List<V1Prize> items = mpPage.getRecords().stream()
                .map(p -> new V1Prize(
                        p.getId().toString(),
                        p.getBatchId().toString(),
                        p.getBatchName(),
                        p.getRewardLevel(),
                        p.getRewardName(),
                        p.getPrizeName(),
                        p.getQuota()
                )).toList();
        return new V1Page<>(items, V1PageInfo.of(page, size, mpPage.getTotal()));
    }

    /** 获取发放记录（分页）。 */
    public V1Page<V1RewardIssue> listIssues(int page, int size, Long rewardId) {
        Page<RewardIssue> mpPage = rewardIssueService.listByPage(page, size, rewardId);
        List<V1RewardIssue> items = mpPage.getRecords().stream()
                .map(V1RewardIssue::from).toList();
        return new V1Page<>(items, V1PageInfo.of(page, size, mpPage.getTotal()));
    }

    /** 获取排行作品列表（用于发奖时选择）。 */
    public List<V1LeaderboardEntry> getRankedWorks(Long batchId, int topN) {
        RankQueryRequest request = new RankQueryRequest();
        request.setBatchId(batchId);
        request.setTopN(topN);
        List<RankVO> ranks = rankService.getRankList(request);
        return ranks.stream().map(r -> new V1LeaderboardEntry(
                r.getRankNo(),
                r.getWorkId().toString(),
                r.getWorkTitle(),
                r.getTechStack(),
                r.getCoverUrl(),
                r.getAdvisor(),
                r.getAvgScore(),
                r.getAvgInnovation(),
                r.getAvgDifficulty(),
                r.getAvgCompletion(),
                r.getAvgPracticality(),
                r.getTeacherCount(),
                r.getSubmitTime() == null ? null : r.getSubmitTime().toString(),
                r.getRewardLevel(),
                r.getRewardName(),
                r.getPrizeName()
        )).toList();
    }
}

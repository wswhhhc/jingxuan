package com.jingxuan.evaluation.internal.application;

import com.jingxuan.evaluation.api.V1LeaderboardEntry;
import com.jingxuan.evaluation.api.V1MyRank;
import com.jingxuan.modules.adapter.StudentRankingFacade;
import com.jingxuan.modules.adapter.TeacherWorkFacade;
import com.jingxuan.modules.rank.dto.RankQueryRequest;
import com.jingxuan.modules.rank.dto.RankVO;
import com.jingxuan.modules.rank.service.RankService;
import com.jingxuan.modules.scorebatch.service.ScoreBatchService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** 排行榜查询用例。 */
@Service
public class LeaderboardQueryService {

    private final RankService rankService;
    private final ScoreBatchService scoreBatchService;
    private final TeacherWorkFacade teacherWorkFacade;
    private final StudentRankingFacade studentRankingFacade;

    public LeaderboardQueryService(RankService rankService,
                                   ScoreBatchService scoreBatchService,
                                   TeacherWorkFacade teacherWorkFacade,
                                   StudentRankingFacade studentRankingFacade) {
        this.rankService = rankService;
        this.scoreBatchService = scoreBatchService;
        this.teacherWorkFacade = teacherWorkFacade;
        this.studentRankingFacade = studentRankingFacade;
    }

    /** 检查排行榜是否已公示。 */
    public boolean isRankPublished(Long batchId) {
        return scoreBatchService.isRankPublished(batchId);
    }

    /** 获取排行榜。 */
    public List<V1LeaderboardEntry> getRankList(Long batchId, int topN, String techStack) {
        RankQueryRequest request = new RankQueryRequest();
        request.setBatchId(batchId);
        request.setTopN(topN);
        request.setTechStack(techStack);
        List<RankVO> ranks = rankService.getRankList(request);
        return ranks.stream().map(this::toLeaderboardEntry).toList();
    }

    /** 获取分类排行。 */
    public List<V1LeaderboardEntry> getCategoryRank(String techStack, Long batchId, int topN) {
        List<RankVO> ranks = rankService.getCategoryRank(techStack, batchId, topN);
        return ranks.stream().map(this::toLeaderboardEntry).toList();
    }

    /** 获取排行分类（技术栈）。 */
    public List<Map<String, String>> listRankingCategories(Long batchId) {
        return teacherWorkFacade.listRankingCategories(batchId);
    }

    /** 获取我的排行。 */
    public List<V1MyRank> getMyRanks(Long userId) {
        return studentRankingFacade.getPublishedRanks(userId).stream()
                .map(s -> new V1MyRank(
                        s.getBatchId().toString(),
                        s.getBatchName(),
                        s.getWorkId().toString(),
                        s.getWorkTitle(),
                        s.getAvgScore(),
                        s.getAvgInnovation(),
                        s.getAvgDifficulty(),
                        s.getAvgCompletion(),
                        s.getAvgPracticality(),
                        s.getTeacherCount(),
                        s.getRankNo()
                ))
                .toList();
    }

    private V1LeaderboardEntry toLeaderboardEntry(RankVO source) {
        return new V1LeaderboardEntry(
                source.getRankNo(),
                source.getWorkId().toString(),
                source.getWorkTitle(),
                source.getTechStack(),
                source.getCoverUrl(),
                source.getAdvisor(),
                source.getAvgScore(),
                source.getAvgInnovation(),
                source.getAvgDifficulty(),
                source.getAvgCompletion(),
                source.getAvgPracticality(),
                source.getTeacherCount(),
                source.getSubmitTime() == null ? null : source.getSubmitTime().toString(),
                source.getRewardLevel(),
                source.getRewardName(),
                source.getPrizeName()
        );
    }
}

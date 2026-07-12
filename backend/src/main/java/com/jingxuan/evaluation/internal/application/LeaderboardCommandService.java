package com.jingxuan.evaluation.internal.application;

import com.jingxuan.modules.rank.service.RankService;
import org.springframework.stereotype.Service;

/** 排行榜命令用例（刷新/清除缓存）。 */
@Service
public class LeaderboardCommandService {

    private final RankService rankService;

    public LeaderboardCommandService(RankService rankService) {
        this.rankService = rankService;
    }

    /** 刷新排行榜缓存。 */
    public void refreshCache(Long batchId) {
        rankService.refreshRankCache(batchId);
    }
}

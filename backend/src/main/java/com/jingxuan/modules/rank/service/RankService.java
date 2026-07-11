package com.jingxuan.modules.rank.service;

import com.jingxuan.modules.rank.dto.RankQueryRequest;
import com.jingxuan.modules.rank.dto.RankVO;

import java.util.List;

public interface RankService {

    /**
     * 获取综合排行榜
     */
    List<RankVO> getRankList(RankQueryRequest request);

    /**
     * 获取分类排行（按技术栈）
     */
    List<RankVO> getCategoryRank(String techStack, Long batchId, int topN);

    /**
     * 手动刷新排行榜缓存（使用最新批次）
     */
    void refreshRankCache();

    /**
     * 手动刷新指定批次的排行榜缓存
     */
    void refreshRankCache(Long batchId);

    /**
     * 清除指定批次的排行榜缓存
     */
    void clearRankCache(Long batchId);
}

package com.jingxuan.modules.rank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingxuan.entity.RankReward;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.Work;
import com.jingxuan.mapper.RankRewardMapper;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.rank.dto.RankQueryRequest;
import com.jingxuan.modules.rank.dto.RankVO;
import com.jingxuan.modules.rank.service.RankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {

    private static final String RANK_CACHE_KEY = "rank:batch:%d:all";
    private static final String RANK_CATEGORY_CACHE_KEY = "rank:batch:%d:cat:%s";
    private static final long RANK_CACHE_TTL = 300;      // 5 分钟
    private static final long EMPTY_CACHE_TTL = 60;       // 空值防穿透 1 分钟

    private final WorkMapper workMapper;
    private final ScoreBatchMapper scoreBatchMapper;
    private final RankRewardMapper rankRewardMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Override
    public List<RankVO> getRankList(RankQueryRequest request) {
        Long batchId = resolveBatchId(request.getBatchId());
        if (batchId == null) {
            return Collections.emptyList();
        }

        String cacheKey = buildCacheKey(batchId, request.getTechStack());

        // 尝试读缓存
        List<RankVO> cached = readFromCache(cacheKey);
        if (cached != null) {
            attachRewards(cached, batchId, request.getTopN());
            return cached;
        }

        // 缓存未命中，查库
        List<RankVO> rankList = computeRankList(batchId, request.getTechStack(), request.getTopN());

        // 回写缓存
        writeToCache(cacheKey, rankList);

        return rankList;
    }

    @Override
    public List<RankVO> getCategoryRank(String techStack, Long batchId, int topN) {
        Long resolvedBatchId = resolveBatchId(batchId);
        if (resolvedBatchId == null) {
            return Collections.emptyList();
        }

        String cacheKey = buildCacheKey(resolvedBatchId, techStack);

        // 尝试读缓存
        List<RankVO> cached = readFromCache(cacheKey);
        if (cached != null) {
            attachRewards(cached, resolvedBatchId, topN);
            return cached;
        }

        // 缓存未命中，查库
        List<RankVO> rankList = computeRankList(resolvedBatchId, techStack, topN);

        // 回写缓存
        writeToCache(cacheKey, rankList);

        return rankList;
    }

    @Override
    public void refreshRankCache() {
        Long batchId = resolveBatchId(null);
        if (batchId == null) {
            log.warn("刷新排行榜缓存失败：无可用批次");
            return;
        }
        refreshRankCache(batchId);
    }

    @Override
    public void refreshRankCache(Long batchId) {
        if (batchId == null) {
            refreshRankCache();
            return;
        }

        log.info("开始刷新排行榜缓存，batchId={}", batchId);

        // 计算综合排行并写入缓存
        List<RankVO> allRank = computeRankList(batchId, null, Integer.MAX_VALUE);
        writeToCache(String.format(RANK_CACHE_KEY, batchId), allRank);
        log.info("综合排行缓存已刷新，共 {} 条", allRank.size());
        log.info("排行榜缓存刷新完成，batchId={}", batchId);
    }

    @Override
    public void clearRankCache(Long batchId) {
        if (batchId == null) return;
        log.info("开始清除排行榜缓存，batchId={}", batchId);

        try {
            // 拼接缓存键模式：整体排行 + 分类排行
            String allKey = String.format(RANK_CACHE_KEY, batchId);
            String catPattern = String.format(RANK_CATEGORY_CACHE_KEY, batchId, "*");
            Set<String> keys = stringRedisTemplate.keys(catPattern);
            if (keys == null) {
                keys = new HashSet<>();
            }
            keys.add(allKey);
            stringRedisTemplate.delete(keys);
            log.info("排行榜缓存已清除，batchId={}, 共 {} 个键", batchId, keys.size());
        } catch (Exception e) {
            log.warn("清除排行榜缓存失败: batchId={}", batchId, e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 从数据库计算排行榜（无缓存）
     */
    private List<RankVO> computeRankList(Long batchId, String techStack, int topN) {
        List<RankVO> rankList = workMapper.selectRankList(batchId, techStack);
        for (int i = 0; i < rankList.size(); i++) {
            rankList.get(i).setRankNo(i + 1);
        }
        attachRewards(rankList, batchId, topN);
        return rankList;
    }

    /**
     * 从缓存读取排行榜，返回 null 表示未命中
     */
    private List<RankVO> readFromCache(String cacheKey) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached == null) {
                return null;
            }
            if (cached.isEmpty()) {
                // 空值缓存（防穿透），视为无数据
                return Collections.emptyList();
            }
            return objectMapper.readValue(cached,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, RankVO.class));
        } catch (Exception e) {
            log.warn("读取排行榜缓存失败，降级查库: key={}", cacheKey, e);
            return null;
        }
    }

    /**
     * 写入缓存，空列表时用短 TTL 防穿透
     */
    private void writeToCache(String cacheKey, List<RankVO> rankList) {
        try {
            if (rankList == null || rankList.isEmpty()) {
                stringRedisTemplate.opsForValue().set(cacheKey, "", EMPTY_CACHE_TTL, TimeUnit.SECONDS);
            } else {
                String json = objectMapper.writeValueAsString(rankList);
                stringRedisTemplate.opsForValue().set(cacheKey, json, RANK_CACHE_TTL, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("写入排行榜缓存失败: key={}", cacheKey, e);
        }
    }

    private String buildCacheKey(Long batchId, String techStack) {
        if (techStack != null && !techStack.isBlank()) {
            return String.format(RANK_CATEGORY_CACHE_KEY, batchId, techStack.trim());
        }
        return String.format(RANK_CACHE_KEY, batchId);
    }

    /**
     * 解析批次ID：如果传入 null 则查询最新批次
     */
    private Long resolveBatchId(Long batchId) {
        if (batchId != null) {
            return batchId;
        }
        ScoreBatch latest = scoreBatchMapper.selectOne(
                new LambdaQueryWrapper<ScoreBatch>()
                        .eq(ScoreBatch::getDeleted, 0)
                        .orderByDesc(ScoreBatch::getCreateTime)
                        .last("LIMIT 1"));
        return latest != null ? latest.getId() : null;
    }

    /**
     * 为排行榜前 N 名附加奖项信息
     */
    private void attachRewards(List<RankVO> rankList, Long batchId, int topN) {
        if (batchId == null || rankList.isEmpty()) {
            return;
        }

        LambdaQueryWrapper<RankReward> rewardWrapper = new LambdaQueryWrapper<>();
        rewardWrapper.eq(RankReward::getDeleted, 0)
                .eq(RankReward::getBatchId, batchId)
                .orderByAsc(RankReward::getRewardLevel);
        List<RankReward> rewards = rankRewardMapper.selectList(rewardWrapper);

        if (rewards.isEmpty()) {
            return;
        }

        Map<Integer, RankReward> rewardMap = rewards.stream()
                .collect(Collectors.toMap(RankReward::getRewardLevel, r -> r, (a, b) -> a));

        rankList.stream()
                .limit(topN)
                .forEach(rank -> {
                    RankReward reward = rewardMap.get(rank.getRankNo());
                    if (reward != null) {
                        rank.setRewardLevel(reward.getRewardName());
                        rank.setRewardName(reward.getRewardName());
                        rank.setPrizeName(reward.getPrizeName());
                    }
                });
    }
}

package com.jingxuan.modules.rank.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingxuan.entity.RankReward;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.mapper.RankRewardMapper;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.rank.dto.RankQueryRequest;
import com.jingxuan.modules.rank.dto.RankVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.type.TypeReference;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankServiceImpl - 排行榜服务")
class RankServiceImplTest {

    @Mock private WorkMapper workMapper;
    @Mock private ScoreBatchMapper scoreBatchMapper;
    @Mock private RankRewardMapper rankRewardMapper;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private NotificationService notificationService;
    @Mock private ValueOperations<String, String> valueOps;

    private RankServiceImpl rankService;

    private static final Long BATCH_ID = 1L;

    @BeforeEach
    void setUp() {
        rankService = new RankServiceImpl(workMapper, scoreBatchMapper, rankRewardMapper,
                stringRedisTemplate, objectMapper, notificationService);
    }

    private RankVO createRankVO(int rankNo, Long workId, String title) {
        RankVO vo = new RankVO();
        vo.setRankNo(rankNo);
        vo.setWorkId(workId);
        vo.setWorkTitle(title);
        vo.setAvgScore(java.math.BigDecimal.valueOf(85.0));
        return vo;
    }

    @Nested
    @DisplayName("排行榜查询")
    class GetRankList {

        @Test
        @DisplayName("缓存命中时直接返回")
        void shouldReturnCachedData() throws Exception {
            // given
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(anyString())).thenReturn("cached_json");
            when(objectMapper.getTypeFactory()).thenReturn(com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance());
            when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.databind.JavaType.class)))
                    .thenReturn(List.of(createRankVO(1, 1L, "作品1")));
            when(rankRewardMapper.selectList(any())).thenReturn(List.of());

            RankQueryRequest request = new RankQueryRequest();
            request.setBatchId(BATCH_ID);

            // when
            List<RankVO> result = rankService.getRankList(request);

            // then
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            verify(workMapper, never()).selectRankList(anyLong(), any());
        }

        @Test
        @DisplayName("缓存未命中时查库并回写缓存")
        void shouldQueryDBWhenCacheMiss() throws Exception {
            // given
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(anyString())).thenReturn(null);

            List<RankVO> dbResult = List.of(
                    createRankVO(1, 1L, "作品A"),
                    createRankVO(2, 2L, "作品B")
            );
            when(workMapper.selectRankList(anyLong(), isNull())).thenReturn(dbResult);
            when(rankRewardMapper.selectList(any())).thenReturn(List.of());
            when(objectMapper.writeValueAsString(any())).thenReturn("[]");

            RankQueryRequest request = new RankQueryRequest();
            request.setBatchId(BATCH_ID);

            // when
            List<RankVO> result = rankService.getRankList(request);

            // then
            assertEquals(2, result.size());
            verify(workMapper).selectRankList(BATCH_ID, null);
            verify(valueOps).set(anyString(), anyString(), eq(300L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("无批次时返回空列表")
        void shouldReturnEmptyWhenNoBatch() {
            when(scoreBatchMapper.selectOne(any())).thenReturn(null);

            RankQueryRequest request = new RankQueryRequest();
            request.setBatchId(null);

            List<RankVO> result = rankService.getRankList(request);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("排行榜缓存管理")
    class CacheManagement {

        @Test
        @DisplayName("刷新排行榜缓存")
        void shouldRefreshRankCache() throws Exception {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

            List<RankVO> rankList = List.of(createRankVO(1, 1L, "作品A"));
            when(workMapper.selectRankList(BATCH_ID, null)).thenReturn(rankList);
            when(rankRewardMapper.selectList(any())).thenReturn(List.of());
            when(objectMapper.writeValueAsString(any())).thenReturn("[]");

            rankService.refreshRankCache(BATCH_ID);

            verify(workMapper).selectRankList(BATCH_ID, null);
            verify(valueOps).set(anyString(), anyString(), eq(300L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("清除排行榜缓存")
        void shouldClearRankCache() {
            when(stringRedisTemplate.keys(anyString())).thenReturn(new java.util.HashSet<>(java.util.Set.of("rank:batch:1:cat:Java")));

            rankService.clearRankCache(BATCH_ID);

            verify(stringRedisTemplate).delete(anySet());
        }
    }

    @Nested
    @DisplayName("分类排行")
    class CategoryRank {

        @Test
        @DisplayName("按技术栈查询排行榜")
        void shouldGetCategoryRank() throws Exception {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(anyString())).thenReturn(null);

            List<RankVO> dbResult = List.of(createRankVO(1, 1L, "Java项目"));
            when(workMapper.selectRankList(BATCH_ID, "Java")).thenReturn(dbResult);
            when(rankRewardMapper.selectList(any())).thenReturn(List.of());
            when(objectMapper.writeValueAsString(any())).thenReturn("[]");

            List<RankVO> result = rankService.getCategoryRank("Java", BATCH_ID, 10);

            assertEquals(1, result.size());
            verify(workMapper).selectRankList(BATCH_ID, "Java");
        }
    }
}

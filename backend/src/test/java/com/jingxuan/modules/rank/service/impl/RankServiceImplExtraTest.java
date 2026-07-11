package com.jingxuan.modules.rank.service.impl;

import tools.jackson.databind.ObjectMapper;
import com.jingxuan.BaseServiceTest;
import com.jingxuan.mapper.RankRewardMapper;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.rank.dto.RankQueryRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("RankServiceImpl - Redis 降级补充测试")
class RankServiceImplExtraTest extends BaseServiceTest {

    @Test
    @DisplayName("Redis 不可用时读取排行榜降级查库且不抛异常")
    void shouldFallbackWhenRedisUnavailableOnRead() {
        WorkMapper workMapper = mock(WorkMapper.class);
        ScoreBatchMapper scoreBatchMapper = mock(ScoreBatchMapper.class);
        RankRewardMapper rankRewardMapper = mock(RankRewardMapper.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        NotificationService notificationService = mock(NotificationService.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenThrow(new RedisConnectionFailureException("redis down"));
        when(workMapper.selectRankList(any(), any())).thenReturn(Collections.emptyList());
        when(rankRewardMapper.selectList(any())).thenReturn(Collections.emptyList());

        RankServiceImpl service = new RankServiceImpl(
                workMapper, scoreBatchMapper, rankRewardMapper, redisTemplate, objectMapper, notificationService);

        RankQueryRequest request = new RankQueryRequest();
        request.setBatchId(1L);
        request.setTopN(10);

        assertDoesNotThrow(() -> service.getRankList(request));
    }

    @Test
    @DisplayName("Redis 不可用时刷新排行榜缓存不抛异常")
    void shouldFallbackWhenRedisUnavailableOnWrite() {
        WorkMapper workMapper = mock(WorkMapper.class);
        ScoreBatchMapper scoreBatchMapper = mock(ScoreBatchMapper.class);
        RankRewardMapper rankRewardMapper = mock(RankRewardMapper.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        NotificationService notificationService = mock(NotificationService.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        doThrow(new RedisConnectionFailureException("redis down"))
                .when(valueOps).set(anyString(), anyString(), anyLong(), any());
        when(workMapper.selectRankList(any(), any())).thenReturn(Collections.emptyList());
        when(rankRewardMapper.selectList(any())).thenReturn(Collections.emptyList());

        RankServiceImpl service = new RankServiceImpl(
                workMapper, scoreBatchMapper, rankRewardMapper, redisTemplate, objectMapper, notificationService);

        assertDoesNotThrow(() -> service.refreshRankCache(1L));
    }
}


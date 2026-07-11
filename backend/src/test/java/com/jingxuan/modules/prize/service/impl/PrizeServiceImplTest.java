package com.jingxuan.modules.prize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.RewardConfig;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.mapper.RewardConfigMapper;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.modules.prize.dto.PrizeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrizeServiceImpl - 奖项配置服务")
class PrizeServiceImplTest {

    @Mock private RewardConfigMapper rewardConfigMapper;
    @Mock private ScoreBatchMapper scoreBatchMapper;

    private PrizeServiceImpl prizeService;

    @BeforeEach
    void setUp() {
        prizeService = new PrizeServiceImpl(rewardConfigMapper, scoreBatchMapper);
    }

    private RewardConfig createConfig(Long id, Long batchId, String level, String prizeName) {
        RewardConfig config = new RewardConfig();
        config.setId(id);
        config.setBatchId(batchId);
        config.setRewardLevel(level);
        config.setRewardName(level + "奖");
        config.setPrizeName(prizeName);
        config.setQuota(3);
        config.setCreateTime(LocalDateTime.now());
        return config;
    }

    @Nested
    @DisplayName("奖项查询")
    class QueryPrize {

        @Test
        @DisplayName("分页查询奖项列表（含批次名称映射）")
        void shouldQueryWithBatchName() {
            RewardConfig config = createConfig(1L, 10L, "一等", "笔记本电脑");
            Page<RewardConfig> pageResult = new Page<>(1, 10, 1);
            pageResult.setRecords(List.of(config));
            when(rewardConfigMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(pageResult);

            ScoreBatch batch = new ScoreBatch();
            batch.setId(10L);
            batch.setBatchName("2024秋季");
            when(scoreBatchMapper.selectBatchIds(List.of(10L)))
                    .thenReturn(List.of(batch));

            PageResult<PrizeVO> result = prizeService.queryPrizeList(1, 10, null);

            assertEquals(1, result.getTotal());
            PrizeVO vo = result.getRecords().get(0);
            assertEquals("一等", vo.getRewardLevel());
            assertEquals("2024秋季", vo.getBatchName());
            assertEquals("笔记本电脑", vo.getPrizeName());
        }

        @Test
        @DisplayName("按批次筛选奖项")
        void shouldFilterByBatch() {
            RewardConfig config = createConfig(2L, 20L, "二等", "平板");
            Page<RewardConfig> pageResult = new Page<>(1, 10, 1);
            pageResult.setRecords(List.of(config));
            when(rewardConfigMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(pageResult);
            when(scoreBatchMapper.selectBatchIds(List.of(20L)))
                    .thenReturn(List.of());

            PageResult<PrizeVO> result = prizeService.queryPrizeList(1, 10, 20L);

            assertEquals(1, result.getTotal());
            assertEquals("平板", result.getRecords().get(0).getPrizeName());
        }

        @Test
        @DisplayName("无数据时返回空列表")
        void shouldReturnEmptyWhenNoData() {
            Page<RewardConfig> emptyPage = new Page<>(1, 10, 0);
            emptyPage.setRecords(List.of());
            when(rewardConfigMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(emptyPage);

            PageResult<PrizeVO> result = prizeService.queryPrizeList(1, 10, null);

            assertEquals(0, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("奖项创建")
    class CreatePrize {

        @Test
        @DisplayName("创建奖项成功")
        void shouldCreatePrize() {
            RewardConfig config = createConfig(null, 10L, "一等", "奖品");
            doAnswer(invocation -> {
                RewardConfig arg = invocation.getArgument(0);
                arg.setId(1L);
                return 1;
            }).when(rewardConfigMapper).insert(any(RewardConfig.class));

            Long id = prizeService.createPrize(config);

            assertEquals(Long.valueOf(1L), id);
            verify(rewardConfigMapper).insert(config);
        }
    }

    @Nested
    @DisplayName("奖项更新")
    class UpdatePrize {

        @Test
        @DisplayName("更新奖项成功")
        void shouldUpdatePrize() {
            RewardConfig config = createConfig(1L, 10L, "特等", "大奖");

            prizeService.updatePrize(config);

            verify(rewardConfigMapper).updateById(config);
        }
    }

    @Nested
    @DisplayName("奖项删除")
    class DeletePrize {

        @Test
        @DisplayName("删除奖项成功")
        void shouldDeletePrize() {
            prizeService.deletePrize(1L);

            verify(rewardConfigMapper).deleteById(1L);
        }
    }
}

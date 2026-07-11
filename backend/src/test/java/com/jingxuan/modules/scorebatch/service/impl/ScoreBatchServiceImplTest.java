package com.jingxuan.modules.scorebatch.service.impl;

import com.jingxuan.common.PageResult;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.Work;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.rank.service.RankService;
import com.jingxuan.modules.task.service.StudentTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jingxuan.entity.ScoreBatch;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreBatchServiceImpl - 评分批次服务")
class ScoreBatchServiceImplTest {

    @Mock private ScoreBatchMapper scoreBatchMapper;
    @Mock private WorkMapper workMapper;
    @Mock private NotificationService notificationService;
    @Mock private RankService rankService;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysDictMapper sysDictMapper;
    @Mock private StudentTaskService studentTaskService;

    @Captor private ArgumentCaptor<ScoreBatch> batchCaptor;

    private ScoreBatchServiceImpl scoreBatchService;

    private static final Long BATCH_ID = 1L;

    @BeforeEach
    void setUp() {
        scoreBatchService = new ScoreBatchServiceImpl(workMapper, notificationService, studentTaskService, rankService, sysUserMapper, sysDictMapper);
        ReflectionTestUtils.setField(scoreBatchService, "baseMapper", scoreBatchMapper);
    }

    @Nested
    @DisplayName("创建批次")
    class CreateBatch {

        @Test
        @DisplayName("成功创建评分批次")
        void shouldCreateBatch() {
            ScoreBatch batch = new ScoreBatch();
            batch.setBatchName("新批次");
            batch.setStartTime(LocalDateTime.now());
            batch.setEndTime(LocalDateTime.now().plusMonths(1));
            // status & rankPublished 为 null，应被填充默认值

            when(scoreBatchMapper.insert(any(ScoreBatch.class))).thenAnswer(inv -> {
                ((ScoreBatch) inv.getArgument(0)).setId(BATCH_ID);
                return 1;
            });

            Long id = scoreBatchService.createBatch(batch);

            assertEquals(BATCH_ID, id);
            verify(scoreBatchMapper).insert(batchCaptor.capture());
            assertEquals(Integer.valueOf(1), batchCaptor.getValue().getStatus());
            assertEquals(Integer.valueOf(0), batchCaptor.getValue().getRankPublished());
        }
    }

    @Nested
    @DisplayName("更新批次")
    class UpdateBatch {

        @Test
        @DisplayName("成功更新批次")
        void shouldUpdateBatch() {
            ScoreBatch batch = new ScoreBatch();
            batch.setId(BATCH_ID);
            batch.setBatchName("更新后的批次");

            scoreBatchService.updateBatch(batch);

            verify(scoreBatchMapper).updateById(batch);
        }
    }

    @Nested
    @DisplayName("查询批次")
    class QueryBatch {

        // queryBatchList 和 getActiveBatch 使用 MyBatis-Plus lambdaQuery，
        // 需要真实 Mapper 代理才能运行，Mock 环境下暂不测试
    }

    @Nested
    @DisplayName("发布/取消排名")
    class PublishRanking {

        @Test
        @DisplayName("成功发布排名")
        void shouldPublishRanking() {
            ScoreBatch batch = new ScoreBatch();
            batch.setId(BATCH_ID);
            when(scoreBatchMapper.selectById(BATCH_ID)).thenReturn(batch);
            when(workMapper.countRankedWorksByBatch(BATCH_ID)).thenReturn(5L);
            when(workMapper.selectList(any())).thenReturn(List.of());

            scoreBatchService.publishRanking(BATCH_ID);

            verify(scoreBatchMapper).updateById(batchCaptor.capture());
            assertEquals(Integer.valueOf(1), batchCaptor.getValue().getRankPublished());
            verify(rankService).refreshRankCache(BATCH_ID);
        }

        @Test
        @DisplayName("无评分数据不可发布排名")
        void shouldThrowWhenNoScoredWorks() {
            ScoreBatch batch = new ScoreBatch();
            batch.setId(BATCH_ID);
            when(scoreBatchMapper.selectById(BATCH_ID)).thenReturn(batch);
            when(workMapper.countRankedWorksByBatch(BATCH_ID)).thenReturn(0L);

            assertThrows(BusinessException.class, () -> scoreBatchService.publishRanking(BATCH_ID));
        }

        @Test
        @DisplayName("取消发布排名")
        void shouldUnpublishRanking() {
            ScoreBatch batch = new ScoreBatch();
            batch.setId(BATCH_ID);
            batch.setRankPublished(1);
            when(scoreBatchMapper.selectById(BATCH_ID)).thenReturn(batch);

            scoreBatchService.unpublishRanking(BATCH_ID);

            verify(scoreBatchMapper).updateById(batchCaptor.capture());
            assertEquals(Integer.valueOf(0), batchCaptor.getValue().getRankPublished());
            verify(rankService).clearRankCache(BATCH_ID);
        }

        @Test
        @DisplayName("批次不存在时抛异常")
        void shouldThrowWhenBatchNotFound() {
            when(scoreBatchMapper.selectById(BATCH_ID)).thenReturn(null);

            assertThrows(BusinessException.class, () -> scoreBatchService.publishRanking(BATCH_ID));
            assertThrows(BusinessException.class, () -> scoreBatchService.unpublishRanking(BATCH_ID));
        }
    }

    @Nested
    @DisplayName("判断方法")
    class JudgeMethods {

        @Test
        @DisplayName("判断排名是否已发布")
        void shouldCheckRankPublished() {
            ScoreBatch published = new ScoreBatch();
            published.setRankPublished(1);
            when(scoreBatchMapper.selectById(BATCH_ID)).thenReturn(published);

            assertTrue(scoreBatchService.isRankPublished(BATCH_ID));

            ScoreBatch unpublished = new ScoreBatch();
            unpublished.setRankPublished(0);
            when(scoreBatchMapper.selectById(2L)).thenReturn(unpublished);

            assertFalse(scoreBatchService.isRankPublished(2L));
        }

        @Test
        @DisplayName("判断当前是否在评分有效期")
        void shouldCheckScoringPeriod() {
            ScoreBatch valid = new ScoreBatch();
            valid.setStartTime(LocalDateTime.now().minusDays(1));
            valid.setEndTime(LocalDateTime.now().plusDays(1));
            when(scoreBatchMapper.selectById(BATCH_ID)).thenReturn(valid);

            assertTrue(scoreBatchService.isWithinScoringPeriod(BATCH_ID));

            ScoreBatch expired = new ScoreBatch();
            expired.setStartTime(LocalDateTime.now().minusDays(10));
            expired.setEndTime(LocalDateTime.now().minusDays(5));
            when(scoreBatchMapper.selectById(2L)).thenReturn(expired);

            assertFalse(scoreBatchService.isWithinScoringPeriod(2L));
        }
    }
}

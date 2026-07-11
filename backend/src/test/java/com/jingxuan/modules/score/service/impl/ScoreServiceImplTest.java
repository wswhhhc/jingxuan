package com.jingxuan.modules.score.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingxuan.config.DeepSeekConfig;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkScore;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkScoreMapper;
import com.jingxuan.modules.log.service.LogService;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.rank.service.RankService;
import com.jingxuan.modules.score.dto.ScoreSubmitRequest;
import com.jingxuan.modules.score.dto.ScoreSummaryVO;
import com.jingxuan.modules.score.dto.ScoreVO;
import com.jingxuan.modules.sensitive.service.DeepSeekReviewService;
import com.jingxuan.modules.sensitive.service.SensitiveWordDFA;
import com.jingxuan.modules.sensitive.service.impl.DeepSeekReviewServiceImpl;
import com.jingxuan.util.DeepSeekApiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ScoreServiceImpl - 评分服务")
class ScoreServiceImplTest {

    @Mock private WorkScoreMapper workScoreMapper;
    @Mock private WorkMapper workMapper;
    @Mock private ScoreBatchMapper scoreBatchMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysDictMapper sysDictMapper;
    @Mock private DeepSeekReviewService deepSeekReviewService;
    @Mock private LogService logService;
    @Mock private RankService rankService;
    @Mock private NotificationService notificationService;
    @Mock private DeepSeekConfig deepSeekConfig;
    @Mock private SensitiveWordDFA sensitiveWordDFA;
    @Mock private DeepSeekApiClient deepSeekApiClient;
    @Mock private HttpResponse<String> httpResponse;

    private ScoreServiceImpl scoreService;

    private static final Long TEACHER_ID = 200L;
    private static final Long WORK_ID = 1L;

    @BeforeEach
    void setUp() {
        scoreService = new ScoreServiceImpl(workScoreMapper, workMapper, scoreBatchMapper);
        ReflectionTestUtils.setField(scoreService, "sysUserMapper", sysUserMapper);
        ReflectionTestUtils.setField(scoreService, "sysDictMapper", sysDictMapper);
        ReflectionTestUtils.setField(scoreService, "deepSeekReviewService", deepSeekReviewService);
        ReflectionTestUtils.setField(scoreService, "logService", logService);
        ReflectionTestUtils.setField(scoreService, "rankService", rankService);
        ReflectionTestUtils.setField(scoreService, "notificationService", notificationService);
    }

    @AfterEach
    void tearDown() {
        // no-op
    }

    private Work createWork(Long id, int status, Long submitterId, Long batchId) {
        Work work = new Work();
        work.setId(id);
        work.setTitle("测试作品");
        work.setStatus(status);
        work.setSubmitterId(submitterId);
        work.setBatchId(batchId);
        return work;
    }

    private ScoreSubmitRequest createValidRequest() {
        ScoreSubmitRequest req = new ScoreSubmitRequest();
        req.setWorkId(WORK_ID);
        req.setInnovation(new BigDecimal("20"));
        req.setDifficulty(new BigDecimal("20"));
        req.setCompletion(new BigDecimal("25"));
        req.setPracticality(new BigDecimal("15"));
        req.setComment("做得好");
        return req;
    }

    @Nested
    @DisplayName("提交评分")
    class SubmitScore {

        @Test
        @DisplayName("成功提交新评分")
        void shouldSubmitNewScore() {
            // given
            Work work = createWork(WORK_ID, 3, 100L, 1L);
            when(workMapper.selectById(WORK_ID)).thenReturn(work);

            ScoreBatch batch = new ScoreBatch();
            batch.setId(1L);
            batch.setStartTime(LocalDateTime.now().minusDays(1));
            batch.setEndTime(LocalDateTime.now().plusDays(1));
            when(scoreBatchMapper.selectById(1L)).thenReturn(batch);

            when(deepSeekReviewService.review(anyString(), anyString()))
                    .thenReturn(DeepSeekReviewService.ReviewResult.pass());
            when(workScoreMapper.selectByWorkAndTeacher(WORK_ID, TEACHER_ID)).thenReturn(null);

            ScoreSubmitRequest request = createValidRequest();

            // when
            scoreService.submitScore(TEACHER_ID, request);

            // then
            verify(workScoreMapper).upsert(anyLong(), eq(WORK_ID), eq(TEACHER_ID), eq(1L),
                    eq(new BigDecimal("20")), eq(new BigDecimal("20")),
                    eq(new BigDecimal("25")), eq(new BigDecimal("15")),
                    eq(new BigDecimal("80")), eq("做得好"));
            verify(logService).recordAction("提交评分", "作品", WORK_ID);
            verify(notificationService).sendNotification(eq(100L), anyString(), contains("80"), anyString(), eq(WORK_ID));
            verify(rankService).refreshRankCache(1L);
        }

        @Test
        @DisplayName("已评分则更新而非新增")
        void shouldUpdateExistingScore() {
            // given
            Work work = createWork(WORK_ID, 3, 100L, 1L);
            when(workMapper.selectById(WORK_ID)).thenReturn(work);

            ScoreBatch batch = new ScoreBatch();
            batch.setId(1L);
            batch.setStartTime(LocalDateTime.now().minusDays(1));
            batch.setEndTime(LocalDateTime.now().plusDays(1));
            when(scoreBatchMapper.selectById(1L)).thenReturn(batch);

            when(deepSeekReviewService.review(anyString(), anyString()))
                    .thenReturn(DeepSeekReviewService.ReviewResult.pass());

            WorkScore existing = new WorkScore();
            existing.setId(10L);
            existing.setWorkId(WORK_ID);
            existing.setTeacherId(TEACHER_ID);
            when(workScoreMapper.selectByWorkAndTeacher(WORK_ID, TEACHER_ID)).thenReturn(existing);

            ScoreSubmitRequest request = createValidRequest();

            // when
            scoreService.submitScore(TEACHER_ID, request);

            // then
            verify(workScoreMapper).upsert(anyLong(), eq(WORK_ID), eq(TEACHER_ID), eq(1L),
                    eq(new BigDecimal("20")), eq(new BigDecimal("20")),
                    eq(new BigDecimal("25")), eq(new BigDecimal("15")),
                    eq(new BigDecimal("80")), eq("做得好"));
        }

        @Test
        @DisplayName("作品不存在抛异常")
        void shouldThrowWhenWorkNotFound() {
            when(workMapper.selectById(WORK_ID)).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> scoreService.submitScore(TEACHER_ID, createValidRequest()));
        }

        @Test
        @DisplayName("未通过审核的作品不可评分")
        void shouldThrowWhenNotApproved() {
            for (int status : List.of(0, 1, 2)) {
                Work work = createWork(WORK_ID, status, 100L, 1L);
                when(workMapper.selectById(WORK_ID)).thenReturn(work);

                assertThrows(BusinessException.class,
                        () -> scoreService.submitScore(TEACHER_ID, createValidRequest()),
                        "状态 " + status + " 不可评分");
            }
        }

        @Test
        @DisplayName("不在评分有效期内抛异常")
        void shouldThrowWhenOutsidePeriod() {
            Work work = createWork(WORK_ID, 3, 100L, 1L);
            when(workMapper.selectById(WORK_ID)).thenReturn(work);

            ScoreBatch batch = new ScoreBatch();
            batch.setId(1L);
            batch.setStartTime(LocalDateTime.now().plusDays(1));
            batch.setEndTime(LocalDateTime.now().plusDays(10));
            when(scoreBatchMapper.selectById(1L)).thenReturn(batch);

            assertThrows(BusinessException.class,
                    () -> scoreService.submitScore(TEACHER_ID, createValidRequest()));
        }

        @Test
        @DisplayName("评语违规抛异常")
        void shouldThrowWhenCommentReviewFails() {
            Work work = createWork(WORK_ID, 3, 100L, 1L);
            when(workMapper.selectById(WORK_ID)).thenReturn(work);

            ScoreBatch batch = new ScoreBatch();
            batch.setId(1L);
            batch.setStartTime(LocalDateTime.now().minusDays(1));
            batch.setEndTime(LocalDateTime.now().plusDays(1));
            when(scoreBatchMapper.selectById(1L)).thenReturn(batch);

            when(deepSeekReviewService.review(anyString(), eq("score")))
                    .thenReturn(DeepSeekReviewService.ReviewResult.fail("abuse", "包含辱骂"));

            assertThrows(BusinessException.class,
                    () -> scoreService.submitScore(TEACHER_ID, createValidRequest()));
        }

        @Test
        @DisplayName("DeepSeek API 不可用且 fallback=bypass 时评分仍成功")
        void shouldSubmitWhenDeepSeekDownAndFallbackBypass() throws Exception {
            Work work = createWork(WORK_ID, 3, 100L, 1L);
            when(workMapper.selectById(WORK_ID)).thenReturn(work);

            ScoreBatch batch = new ScoreBatch();
            batch.setId(1L);
            batch.setStartTime(LocalDateTime.now().minusDays(1));
            batch.setEndTime(LocalDateTime.now().plusDays(1));
            when(scoreBatchMapper.selectById(1L)).thenReturn(batch);

            DeepSeekReviewServiceImpl realReviewService =
                    new DeepSeekReviewServiceImpl(deepSeekConfig, deepSeekApiClient, new ObjectMapper(), sensitiveWordDFA);
            ReflectionTestUtils.setField(scoreService, "deepSeekReviewService", realReviewService);

            when(sensitiveWordDFA.contains(anyString())).thenReturn(false);
            when(deepSeekConfig.getApiKey()).thenReturn("sk-test");
            when(deepSeekConfig.getApiUrl()).thenReturn("https://api.deepseek.com/v1/chat/completions");
            when(deepSeekConfig.getModel()).thenReturn("deepseek-chat");
            when(deepSeekConfig.getTimeout()).thenReturn(1000);
            when(deepSeekConfig.getFallback()).thenReturn("bypass");
            when(deepSeekApiClient.post(any(java.util.Map.class)))
                    .thenThrow(new RuntimeException("api down"));
            when(workScoreMapper.selectByWorkAndTeacher(WORK_ID, TEACHER_ID)).thenReturn(null);

            scoreService.submitScore(TEACHER_ID, createValidRequest());

            verify(workScoreMapper).upsert(anyLong(), anyLong(), anyLong(), any(),
                    any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("DeepSeek API 不可用且 fallback=reject 时评分失败")
        void shouldRejectWhenDeepSeekDownAndFallbackReject() throws Exception {
            Work work = createWork(WORK_ID, 3, 100L, 1L);
            when(workMapper.selectById(WORK_ID)).thenReturn(work);

            ScoreBatch batch = new ScoreBatch();
            batch.setId(1L);
            batch.setStartTime(LocalDateTime.now().minusDays(1));
            batch.setEndTime(LocalDateTime.now().plusDays(1));
            when(scoreBatchMapper.selectById(1L)).thenReturn(batch);

            DeepSeekReviewServiceImpl realReviewService =
                    new DeepSeekReviewServiceImpl(deepSeekConfig, deepSeekApiClient, new ObjectMapper(), sensitiveWordDFA);
            ReflectionTestUtils.setField(scoreService, "deepSeekReviewService", realReviewService);

            when(sensitiveWordDFA.contains(anyString())).thenReturn(false);
            when(deepSeekConfig.getApiKey()).thenReturn("sk-test");
            when(deepSeekConfig.getApiUrl()).thenReturn("https://api.deepseek.com/v1/chat/completions");
            when(deepSeekConfig.getModel()).thenReturn("deepseek-chat");
            when(deepSeekConfig.getTimeout()).thenReturn(1000);
            when(deepSeekConfig.getFallback()).thenReturn("reject");
            when(deepSeekApiClient.post(any(java.util.Map.class)))
                    .thenThrow(new RuntimeException("api down"));

            assertThrows(BusinessException.class,
                    () -> scoreService.submitScore(TEACHER_ID, createValidRequest()));
        }

        @Test
        @DisplayName("同一作品多人同时评分时不应全部失败")
        void shouldHandleConcurrentScoring() throws Exception {
            Work work = createWork(WORK_ID, 3, 100L, 1L);
            when(workMapper.selectById(WORK_ID)).thenReturn(work);

            ScoreBatch batch = new ScoreBatch();
            batch.setId(1L);
            batch.setStartTime(LocalDateTime.now().minusDays(1));
            batch.setEndTime(LocalDateTime.now().plusDays(1));
            when(scoreBatchMapper.selectById(1L)).thenReturn(batch);
            when(deepSeekReviewService.review(anyString(), anyString()))
                    .thenReturn(DeepSeekReviewService.ReviewResult.pass());
            when(workScoreMapper.selectByWorkAndTeacher(eq(WORK_ID), anyLong())).thenReturn(null);

            CountDownLatch latch = new CountDownLatch(1);
            var pool = Executors.newFixedThreadPool(3);
            List<Long> teacherIds = List.of(200L, 201L, 202L);
            List<Future<Boolean>> futures = teacherIds.stream().map(id -> pool.submit(() -> {
                latch.await();
                try {
                    scoreService.submitScore(id, createValidRequest());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            })).toList();

            latch.countDown();
            int success = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) success++;
            }
            pool.shutdown();

            assertTrue(success >= 1, "并发评分至少应有一个成功");
        }
    }

    @Nested
    @DisplayName("评分查询")
    class QueryScore {

        @Test
        @DisplayName("获取教师评分")
        void shouldGetTeacherScore() {
            WorkScore score = new WorkScore();
            score.setId(1L);
            score.setWorkId(WORK_ID);
            score.setTeacherId(TEACHER_ID);
            score.setTotal(new BigDecimal("85"));
            when(workScoreMapper.selectByWorkAndTeacher(WORK_ID, TEACHER_ID)).thenReturn(score);

            SysUser teacher = new SysUser();
            teacher.setId(TEACHER_ID);
            teacher.setRealName("张教授");
            when(sysUserMapper.selectById(TEACHER_ID)).thenReturn(teacher);

            ScoreVO vo = scoreService.getTeacherScore(WORK_ID, TEACHER_ID);

            assertNotNull(vo);
            assertEquals("85", vo.getTotal().stripTrailingZeros().toPlainString());
            assertEquals("张教授", vo.getTeacherName());
        }

        @Test
        @DisplayName("获取作品评分摘要")
        void shouldGetScoreSummary() {
            WorkScore s1 = new WorkScore();
            s1.setInnovation(new BigDecimal("20"));
            s1.setDifficulty(new BigDecimal("20"));
            s1.setCompletion(new BigDecimal("25"));
            s1.setPracticality(new BigDecimal("15"));
            s1.setTotal(new BigDecimal("80"));

            WorkScore s2 = new WorkScore();
            s2.setInnovation(new BigDecimal("22"));
            s2.setDifficulty(new BigDecimal("23"));
            s2.setCompletion(new BigDecimal("27"));
            s2.setPracticality(new BigDecimal("18"));
            s2.setTotal(new BigDecimal("90"));

            when(workScoreMapper.selectByWorkId(WORK_ID)).thenReturn(List.of(s1, s2));

            Work work = createWork(WORK_ID, 3, 100L, 1L);
            when(workMapper.selectById(WORK_ID)).thenReturn(work);

            ScoreSummaryVO summary = scoreService.getScoreSummary(WORK_ID);

            assertNotNull(summary);
            assertEquals("测试作品", summary.getWorkTitle());
            assertEquals(2, summary.getTeacherCount());
            assertEquals(0, new BigDecimal("21.00").compareTo(summary.getAvgInnovation()));
            assertEquals(0, new BigDecimal("21.50").compareTo(summary.getAvgDifficulty()));
            assertEquals(0, new BigDecimal("26.00").compareTo(summary.getAvgCompletion()));
            assertEquals(0, new BigDecimal("16.50").compareTo(summary.getAvgPracticality()));
            assertEquals(0, new BigDecimal("85.00").compareTo(summary.getAvgTotal()));
        }

        @Test
        @DisplayName("无评分时返回 null")
        void shouldReturnNullWhenNoScores() {
            when(workScoreMapper.selectByWorkId(WORK_ID)).thenReturn(List.of());

            assertNull(scoreService.getScoreSummary(WORK_ID));
        }
    }
}

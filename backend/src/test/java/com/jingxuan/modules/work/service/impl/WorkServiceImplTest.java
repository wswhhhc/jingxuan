package com.jingxuan.modules.work.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkAttachment;
import com.jingxuan.entity.WorkMember;
import com.jingxuan.enums.AuditStatusEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.*;
import com.jingxuan.modules.log.service.LogService;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.task.service.StudentTaskService;
import com.jingxuan.modules.sensitive.service.DeepSeekReviewService;
import com.jingxuan.modules.task.service.StudentTaskService;
import com.jingxuan.modules.work.dto.WorkMemberDTO;
import com.jingxuan.modules.work.dto.WorkRequest;
import com.jingxuan.modules.work.service.WorkMemberService;
import com.jingxuan.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WorkServiceImpl - 作品服务")
class WorkServiceImplTest {

    @Mock private WorkMapper workMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private WorkMemberService workMemberService;
    @Mock private WorkMemberMapper workMemberMapper;
    @Mock private WorkAttachmentMapper workAttachmentMapper;
    @Mock private WorkPublishMapper workPublishMapper;
    @Mock private ScoreBatchMapper scoreBatchMapper;
    @Mock private SysDictMapper sysDictMapper;
    @Mock private LogService logService;
    @Mock private DeepSeekReviewService deepSeekReviewService;
    @Mock private StudentTaskService studentTaskService;
    @Mock private WorkAuditMapper workAuditMapper;
    @Mock private WorkCommentMapper workCommentMapper;
    @Mock private WorkLikeMapper workLikeMapper;
    @Mock private WorkScoreMapper workScoreMapper;
    @Mock private WorkTagMapper workTagMapper;
    @Mock private RewardIssueMapper rewardIssueMapper;
    @Mock private NotificationService notificationService;

    @Captor private ArgumentCaptor<Work> workCaptor;

    private WorkServiceImpl workService;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    private static final Long CURRENT_USER_ID = 100L;

    @BeforeEach
    void setUp() {
        WorkContentReviewService contentReviewService = new WorkContentReviewService(deepSeekReviewService);
        WorkAttachmentBindingService attachmentBindingService =
                new WorkAttachmentBindingService(workAttachmentMapper);
        WorkMemberPolicyService memberPolicyService =
                new WorkMemberPolicyService(sysUserMapper, workMemberMapper, workMapper);
        WorkQueryValidator queryValidator = new WorkQueryValidator();

        workService = new WorkServiceImpl(sysUserMapper, workMemberService, workMemberMapper,
                workAttachmentMapper, workPublishMapper, scoreBatchMapper, logService,
                contentReviewService, attachmentBindingService, memberPolicyService, queryValidator,
                studentTaskService, notificationService, workAuditMapper, workCommentMapper,
                workLikeMapper, workScoreMapper, workTagMapper, rewardIssueMapper);
        // baseMapper（从 ServiceImpl 继承）
        ReflectionTestUtils.setField(workService, "baseMapper", workMapper);

        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::requireCurrentUserId).thenReturn(CURRENT_USER_ID);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    private Work createWork(Long id, int status, Long submitterId) {
        Work work = new Work();
        work.setId(id);
        work.setTitle("测试作品");
        work.setSummary("测试简介");
        work.setStatus(status);
        work.setSubmitterId(submitterId);
        work.setBatchId(1L);
        return work;
    }

    private WorkMemberDTO createMember(String name, Long studentId) {
        WorkMemberDTO dto = new WorkMemberDTO();
        dto.setStudentName(name);
        dto.setStudentNo("2022001");
        dto.setClassName("软件1班");
        dto.setIsLeader(1);
        dto.setStudentId(studentId);
        return dto;
    }

    @Nested
    @DisplayName("创建作品")
    class CreateWork {

        @Test
        @DisplayName("成功创建作品")
        void shouldCreateWork() {
            // given
            when(scoreBatchMapper.selectOne(any())).thenReturn(null);
            when(deepSeekReviewService.review(anyString(), anyString()))
                    .thenReturn(DeepSeekReviewService.ReviewResult.pass());
            when(workMapper.insert((Work) any())).thenAnswer(inv -> {
                ((Work) inv.getArgument(0)).setId(1L);
                return 1;
            });

            WorkRequest request = new WorkRequest();
            request.setTitle("新作品");
            request.setSummary("简介");
            request.setTechStack("Java/Spring Boot");

            // when
            Long workId = workService.createWork(request);

            // then
            assertNotNull(workId);
            verify(workMapper).insert(workCaptor.capture());
            assertEquals(AuditStatusEnum.DRAFT.getValue(), workCaptor.getValue().getStatus());
            assertEquals(CURRENT_USER_ID, workCaptor.getValue().getSubmitterId());
            verify(deepSeekReviewService).review(anyString(), eq("work"));
        }

        @Test
        @DisplayName("同一批次中重复提交抛异常")
        void shouldThrowWhenDuplicateInBatch() {
            // given
            ScoreBatch activeBatch = new ScoreBatch();
            activeBatch.setId(1L);
            activeBatch.setStatus(1);
            when(scoreBatchMapper.selectOne(any())).thenReturn(activeBatch);
            when(workMapper.selectCount(any())).thenReturn(1L); // 已存在作品

            WorkRequest request = new WorkRequest();
            request.setTitle("重复作品");

            // when/then
            assertThrows(BusinessException.class, () -> workService.createWork(request));
            verify(workMapper, never()).insert((Work) any());
        }

        @Test
        @DisplayName("团队成员已在同一批次其他作品中抛异常")
        void shouldThrowWhenMemberAlreadyInBatch() {
            // given
            ScoreBatch activeBatch = new ScoreBatch();
            activeBatch.setId(1L);
            activeBatch.setStatus(1);
            when(scoreBatchMapper.selectOne(any())).thenReturn(activeBatch);
            when(workMapper.selectCount(any())).thenReturn(0L); // 提交者无作品
            lenient().when(workMapper.selectObjs(any())).thenReturn(List.of(99L)); // 批次内有其他作品
            when(workMemberMapper.selectCount(any())).thenReturn(1L); // 成员已在其他作品中
            when(deepSeekReviewService.review(anyString(), anyString()))
                    .thenReturn(DeepSeekReviewService.ReviewResult.pass());

            WorkRequest request = new WorkRequest();
            request.setTitle("新作品");
            request.setMembers(List.of(createMember("张三", 101L)));

            // when/then
            assertThrows(BusinessException.class, () -> workService.createWork(request));
        }

        @Test
        @DisplayName("内容审核不通过抛异常")
        void shouldThrowWhenReviewFails() {
            // given
            when(scoreBatchMapper.selectOne(any())).thenReturn(null);
            when(deepSeekReviewService.review(anyString(), anyString()))
                    .thenReturn(DeepSeekReviewService.ReviewResult.fail("content", "包含违规内容"));

            WorkRequest request = new WorkRequest();
            request.setTitle("违规作品");

            // when/then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workService.createWork(request));
            assertTrue(ex.getMessage().contains("违规"));
            verify(workMapper, never()).insert((Work) any());
        }

        @Test
        @DisplayName("附件已被其他作品占用抛异常")
        void shouldThrowWhenAttachmentOccupied() {
            // given
            when(scoreBatchMapper.selectOne(any())).thenReturn(null);
            when(deepSeekReviewService.review(anyString(), anyString()))
                    .thenReturn(DeepSeekReviewService.ReviewResult.pass());
            when(workAttachmentMapper.selectCount(any())).thenReturn(1L); // 附件被占用

            WorkRequest request = new WorkRequest();
            request.setTitle("新作品");
            request.setAttachmentIds(List.of("99"));

            // when/then
            assertThrows(BusinessException.class, () -> workService.createWork(request));
        }
    }

    @Nested
    @DisplayName("更新作品")
    class UpdateWork {

        @Test
        @DisplayName("成功更新草稿作品")
        void shouldUpdateDraftWork() {
            // given
            Work work = createWork(1L, AuditStatusEnum.DRAFT.getValue(), CURRENT_USER_ID);
            when(workMapper.selectById(1L)).thenReturn(work);
            when(deepSeekReviewService.review(anyString(), anyString()))
                    .thenReturn(DeepSeekReviewService.ReviewResult.pass());

            WorkRequest request = new WorkRequest();
            request.setTitle("新标题");
            request.setSummary("新简介");

            // when
            workService.updateWork(1L, request);

            // then
            verify(workMapper).updateById(workCaptor.capture());
            assertEquals("新标题", workCaptor.getValue().getTitle());
            verify(deepSeekReviewService).review(anyString(), eq("work"));
        }

        @Test
        @DisplayName("已提交状态不可编辑")
        void shouldThrowWhenNotEditable() {
            for (int status : List.of(1, 3)) {
                Work work = createWork(1L, status, CURRENT_USER_ID);
                when(workMapper.selectById(1L)).thenReturn(work);

                assertThrows(BusinessException.class,
                        () -> workService.updateWork(1L, new WorkRequest()),
                        "状态 " + status + " 应不可编辑");
            }
        }

        @Test
        @DisplayName("非提交者不可编辑")
        void shouldThrowWhenNotOwner() {
            Work work = createWork(1L, AuditStatusEnum.DRAFT.getValue(), 999L);
            when(workMapper.selectById(1L)).thenReturn(work);

            assertThrows(BusinessException.class,
                    () -> workService.updateWork(1L, new WorkRequest()));
        }
    }

    @Nested
    @DisplayName("提交审核")
    class SubmitWork {

        @Test
        @DisplayName("成功提交审核")
        void shouldSubmitWork() {
            // given
            Work work = createWork(1L, AuditStatusEnum.DRAFT.getValue(), CURRENT_USER_ID);
            work.setBatchId(null);
            work.setPreviewUrl("http://example.com");
            when(workMapper.selectById(1L)).thenReturn(work);
            // 模拟提交时已上传源代码压缩包和演示视频
            WorkAttachment sourceArchive = new WorkAttachment();
            sourceArchive.setFileType("zip");
            sourceArchive.setWorkId(1L);
            WorkAttachment video = new WorkAttachment();
            video.setFileType("mp4");
            video.setWorkId(1L);
            when(workAttachmentMapper.selectList(any())).thenReturn(List.of(sourceArchive, video));
            when(deepSeekReviewService.review(anyString(), anyString()))
                    .thenReturn(DeepSeekReviewService.ReviewResult.pass());

            // when
            workService.submitWork(1L);

            // then
            verify(workMapper).updateById(workCaptor.capture());
            assertEquals(AuditStatusEnum.SUBMITTED.getValue(), workCaptor.getValue().getStatus());
            assertNotNull(workCaptor.getValue().getSubmitTime());
            verify(logService).recordAction("提交审核", "作品", 1L);
        }

        @Test
        @DisplayName("附件为空不可提交")
        void shouldThrowWhenNoAttachment() {
            Work work = createWork(1L, AuditStatusEnum.DRAFT.getValue(), CURRENT_USER_ID);
            work.setPreviewUrl("http://example.com");
            when(workMapper.selectById(1L)).thenReturn(work);
            when(workAttachmentMapper.selectList(any())).thenReturn(List.of()); // 无附件

            assertThrows(BusinessException.class, () -> workService.submitWork(1L));
        }

        @Test
        @DisplayName("仅草稿或已驳回可提交")
        void shouldThrowWhenNotDraftOrRejected() {
            for (int status : List.of(1, 3)) {
                Work work = createWork(1L, status, CURRENT_USER_ID);
                when(workMapper.selectById(1L)).thenReturn(work);

                assertThrows(BusinessException.class,
                        () -> workService.submitWork(1L),
                        "状态 " + status + " 应不可提交");
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LambdaUpdateWrapper createMockLambdaUpdate() {
        LambdaUpdateWrapper wrapper = mock(LambdaUpdateWrapper.class);
        when(wrapper.set(any(), any())).thenReturn(wrapper);
        when(wrapper.eq(any(), any())).thenReturn(wrapper);
        return wrapper;
    }

    @Nested
    @DisplayName("删除作品")
    class DeleteWork {

        @Test
        @DisplayName("成功删除草稿")
        void shouldDeleteDraftWork() {
            // given
            Work work = createWork(1L, AuditStatusEnum.DRAFT.getValue(), CURRENT_USER_ID);
            when(workMapper.selectById(1L)).thenReturn(work);

            // 预先创建 mock wrapper（避免在 thenReturn 中嵌套 when）
            LambdaUpdateWrapper mockWrapper = createMockLambdaUpdate();

            // when
            try (MockedStatic<Wrappers> wrappersMock = mockStatic(Wrappers.class)) {
                wrappersMock.when(() -> { Wrappers.lambdaUpdate(); }).thenReturn(mockWrapper);
                workService.deleteWork(1L);
            }

            // then
            verify(workAttachmentMapper).update(any());
            verify(workMapper).deleteById(1L);
        }

        @Test
        @DisplayName("已提交或已通过不可删除")
        void shouldThrowWhenNotDeletable() {
            for (int status : List.of(1, 3)) {
                Work work = createWork(1L, status, CURRENT_USER_ID);
                when(workMapper.selectById(1L)).thenReturn(work);

                assertThrows(BusinessException.class,
                        () -> workService.deleteWork(1L),
                        "状态 " + status + " 应不可删除");
            }
        }
    }
}

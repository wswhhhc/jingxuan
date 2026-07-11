package com.jingxuan.modules.audit.service.impl;

import com.jingxuan.common.PageResult;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkAudit;
import com.jingxuan.enums.AuditStatusEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkAuditMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkPublishMapper;
import com.jingxuan.modules.audit.dto.AuditHistoryVO;
import com.jingxuan.modules.audit.dto.AuditRequest;
import com.jingxuan.modules.log.service.LogService;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.publish.service.PublishService;
import com.jingxuan.modules.task.service.StudentTaskService;
import com.jingxuan.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditServiceImpl - 审核服务")
class AuditServiceImplTest {

    @Mock private WorkMapper workMapper;
    @Mock private WorkAuditMapper workAuditMapper;
    @Mock private WorkPublishMapper workPublishMapper;
    @Mock private PublishService publishService;
    @Mock private NotificationService notificationService;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private LogService logService;
    @Mock private StudentTaskService studentTaskService;

    @Captor private ArgumentCaptor<Work> workCaptor;
    @Captor private ArgumentCaptor<WorkAudit> auditCaptor;

    private AuditServiceImpl auditService;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        auditService = new AuditServiceImpl(workMapper, workAuditMapper, workPublishMapper,
                publishService, notificationService, sysUserMapper, logService, studentTaskService);
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::requireCurrentUserId).thenReturn(300L);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    private Work createWork(Long id, int status) {
        Work work = new Work();
        work.setId(id);
        work.setTitle("测试作品");
        work.setStatus(status);
        work.setSubmitterId(100L);
        return work;
    }

    @Nested
    @DisplayName("审核通过")
    class Approve {

        @Test
        @DisplayName("成功通过审核")
        void shouldApproveWork() {
            // given
            Long workId = 1L;
            Work work = createWork(workId, AuditStatusEnum.SUBMITTED.getValue());
            when(workMapper.selectById(workId)).thenReturn(work);

            AuditRequest request = new AuditRequest();
            request.setWorkId(workId);

            // when
            auditService.approve(request);

            // then
            verify(workMapper).updateById(workCaptor.capture());
            assertEquals(AuditStatusEnum.APPROVED.getValue(), workCaptor.getValue().getStatus());

            verify(workAuditMapper).insert(auditCaptor.capture());
            assertEquals(1, auditCaptor.getValue().getResult());
            assertEquals(workId, auditCaptor.getValue().getWorkId());

            verify(publishService).initPublish(workId);
            verify(notificationService).sendNotification(eq(100L), anyString(), anyString(), eq("audit"), eq(workId));
            verify(logService).recordAction(anyString(), anyString(), eq(workId));
        }

        @Test
        @DisplayName("作品不存在时抛异常")
        void shouldThrowWhenWorkNotFound() {
            when(workMapper.selectById(anyLong())).thenReturn(null);

            AuditRequest request = new AuditRequest();
            request.setWorkId(999L);

            assertThrows(BusinessException.class, () -> auditService.approve(request));
            verify(workMapper, never()).updateById(any(Work.class));
            verify(workAuditMapper, never()).insert(any(WorkAudit.class));
        }

        @Test
        @DisplayName("非提交状态不可通过")
        void shouldThrowWhenNotSubmitted() {
            for (int invalidStatus : List.of(0, 2, 3)) {
                Work work = createWork(1L, invalidStatus);
                when(workMapper.selectById(1L)).thenReturn(work);

                AuditRequest request = new AuditRequest();
                request.setWorkId(1L);

                assertThrows(BusinessException.class, () -> auditService.approve(request),
                        "状态 " + invalidStatus + " 应不可审核通过");
            }
        }
    }

    @Nested
    @DisplayName("审核驳回")
    class Reject {

        @Test
        @DisplayName("成功驳回作品")
        void shouldRejectWork() {
            Long workId = 1L;
            Work work = createWork(workId, AuditStatusEnum.SUBMITTED.getValue());
            when(workMapper.selectById(workId)).thenReturn(work);

            AuditRequest request = new AuditRequest();
            request.setWorkId(workId);
            request.setReason("技术文档缺失");

            auditService.reject(request);

            verify(workMapper).updateById(workCaptor.capture());
            assertEquals(AuditStatusEnum.REJECTED.getValue(), workCaptor.getValue().getStatus());

            verify(workAuditMapper).insert(auditCaptor.capture());
            assertEquals(0, auditCaptor.getValue().getResult());
            assertEquals("技术文档缺失", auditCaptor.getValue().getReason());

            verify(notificationService).sendNotification(eq(100L), anyString(), contains("技术文档缺失"), anyString(), eq(workId));
        }

        @Test
        @DisplayName("作品不存在时抛异常")
        void shouldThrowWhenWorkNotFound() {
            when(workMapper.selectById(anyLong())).thenReturn(null);

            AuditRequest request = new AuditRequest();
            request.setWorkId(999L);

            assertThrows(BusinessException.class, () -> auditService.reject(request));
        }
    }

    @Nested
    @DisplayName("审核历史查询")
    class QueryHistory {

        @Test
        @DisplayName("查询审核历史")
        void shouldQueryHistory() {
            // given
            Long workId = 1L;
            Work work = createWork(workId, 3);
            when(workMapper.selectById(workId)).thenReturn(work);

            com.baomidou.mybatisplus.extension.plugins.pagination.Page<WorkAudit> mpPage =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
            WorkAudit audit1 = new WorkAudit();
            audit1.setId(1L);
            audit1.setWorkId(workId);
            audit1.setAuditorId(300L);
            audit1.setResult(1);
            audit1.setReason(null);
            audit1.setAuditTime(java.time.LocalDateTime.now());
            mpPage.setRecords(List.of(audit1));
            mpPage.setTotal(1);

            when(workAuditMapper.selectPage(any(), any())).thenReturn(mpPage);

            com.jingxuan.entity.SysUser admin = new com.jingxuan.entity.SysUser();
            admin.setId(300L);
            admin.setRealName("管理员");
            when(sysUserMapper.selectById(300L)).thenReturn(admin);

            // when
            PageResult<AuditHistoryVO> result = auditService.queryHistory(workId, 1, 10);

            // then
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getRecords().size());
            assertEquals("管理员", result.getRecords().get(0).getAuditorName());
            assertEquals("通过", result.getRecords().get(0).getResultLabel());
        }
    }
}

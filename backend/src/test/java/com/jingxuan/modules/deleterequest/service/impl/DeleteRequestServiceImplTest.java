package com.jingxuan.modules.deleterequest.service.impl;

import com.jingxuan.entity.DeleteRequest;
import com.jingxuan.entity.Work;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.DeleteRequestMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.work.service.WorkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DeleteRequestServiceImpl - 删除申请服务")
class DeleteRequestServiceImplTest {

    @Mock private DeleteRequestMapper deleteRequestMapper;
    @Mock private WorkMapper workMapper;
    @Mock private WorkService workService;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private NotificationService notificationService;

    @Captor private ArgumentCaptor<DeleteRequest> requestCaptor;

    private DeleteRequestServiceImpl deleteRequestService;

    private static final Long WORK_ID = 1L;
    private static final Long STUDENT_ID = 100L;
    private static final Long ADMIN_ID = 200L;

    @BeforeEach
    void setUp() {
        deleteRequestService = new DeleteRequestServiceImpl(
                workMapper, workService, sysUserMapper, notificationService);
        ReflectionTestUtils.setField(deleteRequestService, "baseMapper", deleteRequestMapper);
    }

    private Work createApprovedWork(Long id, Long submitterId) {
        Work work = new Work();
        work.setId(id);
        work.setTitle("测试作品");
        work.setStatus(3); // 已审核通过
        work.setSubmitterId(submitterId);
        return work;
    }

    @Nested
    @DisplayName("提交删除申请")
    class SubmitRequest {

        @Test
        @DisplayName("成功提交申请")
        void shouldSubmitSuccessfully() {
            when(workMapper.selectById(WORK_ID)).thenReturn(createApprovedWork(WORK_ID, STUDENT_ID));
            when(deleteRequestMapper.insert(any(DeleteRequest.class))).thenReturn(1);
            when(deleteRequestMapper.selectCount(any())).thenReturn(0L);

            deleteRequestService.submitRequest(WORK_ID, STUDENT_ID, "作品已完成，申请删除");

            verify(deleteRequestMapper).insert(requestCaptor.capture());
            DeleteRequest captured = requestCaptor.getValue();
            assertEquals(WORK_ID, captured.getWorkId());
            assertEquals(STUDENT_ID, captured.getStudentId());
            assertEquals("作品已完成，申请删除", captured.getReason());
            assertEquals(0, captured.getStatus());
        }

        @Test
        @DisplayName("作品不存在抛异常")
        void shouldThrowWhenWorkNotFound() {
            when(workMapper.selectById(WORK_ID)).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> deleteRequestService.submitRequest(WORK_ID, STUDENT_ID, "原因"));
        }

        @Test
        @DisplayName("仅已通过的作品可申请删除")
        void shouldThrowWhenNotApproved() {
            Work draft = createApprovedWork(WORK_ID, STUDENT_ID);
            draft.setStatus(0);
            when(workMapper.selectById(WORK_ID)).thenReturn(draft);

            assertThrows(BusinessException.class,
                    () -> deleteRequestService.submitRequest(WORK_ID, STUDENT_ID, "原因"));
        }

        @Test
        @DisplayName("不能申请删除别人的作品")
        void shouldThrowWhenNotOwner() {
            when(workMapper.selectById(WORK_ID)).thenReturn(createApprovedWork(WORK_ID, 999L));

            assertThrows(BusinessException.class,
                    () -> deleteRequestService.submitRequest(WORK_ID, STUDENT_ID, "原因"));
        }

        @Test
        @DisplayName("已有待处理的申请时不能重复提交")
        void shouldThrowWhenPendingExists() {
            when(workMapper.selectById(WORK_ID)).thenReturn(createApprovedWork(WORK_ID, STUDENT_ID));
            when(deleteRequestMapper.selectCount(any())).thenReturn(1L); // 已有待处理申请

            assertThrows(BusinessException.class,
                    () -> deleteRequestService.submitRequest(WORK_ID, STUDENT_ID, "原因"));
        }
    }

    @Nested
    @DisplayName("审批删除申请")
    class ApproveRequest {

        @Test
        @DisplayName("同意删除申请后执行作品删除并通知学生")
        void shouldApproveAndDeleteWork() {
            DeleteRequest request = new DeleteRequest();
            request.setId(10L);
            request.setWorkId(WORK_ID);
            request.setStudentId(STUDENT_ID);
            request.setStatus(0);
            request.setDeleted(0);
            when(deleteRequestMapper.selectById(10L)).thenReturn(request);

            deleteRequestService.approve(10L, ADMIN_ID);

            verify(deleteRequestMapper).updateById(requestCaptor.capture());
            assertEquals(1, requestCaptor.getValue().getStatus()); // 已同意
            verify(workService).adminDeleteWork(WORK_ID); // 执行删除
            verify(notificationService).sendNotification(eq(STUDENT_ID), anyString(), anyString(), eq("delete_approved"), eq(WORK_ID));
        }

        @Test
        @DisplayName("已处理的申请不能再次审批")
        void shouldThrowWhenAlreadyProcessed() {
            DeleteRequest request = new DeleteRequest();
            request.setId(10L);
            request.setStatus(1); // 已同意
            request.setDeleted(0);
            when(deleteRequestMapper.selectById(10L)).thenReturn(request);

            assertThrows(BusinessException.class,
                    () -> deleteRequestService.approve(10L, ADMIN_ID));
        }
    }

    @Nested
    @DisplayName("拒绝删除申请")
    class RejectRequest {

        @Test
        @DisplayName("拒绝后设置回复并通知学生")
        void shouldRejectAndNotify() {
            DeleteRequest request = new DeleteRequest();
            request.setId(10L);
            request.setWorkId(WORK_ID);
            request.setStudentId(STUDENT_ID);
            request.setStatus(0);
            request.setDeleted(0);
            when(deleteRequestMapper.selectById(10L)).thenReturn(request);

            deleteRequestService.reject(10L, ADMIN_ID, "作品质量优秀，建议保留");

            verify(deleteRequestMapper).updateById(requestCaptor.capture());
            DeleteRequest updated = requestCaptor.getValue();
            assertEquals(2, updated.getStatus()); // 已拒绝
            assertEquals("作品质量优秀，建议保留", updated.getAdminReply());
            verify(notificationService).sendNotification(eq(STUDENT_ID), anyString(), contains("作品质量优秀"), eq("delete_rejected"), eq(WORK_ID));
        }
    }
}

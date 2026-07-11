package com.jingxuan.modules.publish.service.impl;

import com.jingxuan.constant.CommonConstants;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkPublish;
import com.jingxuan.enums.AuditStatusEnum;
import com.jingxuan.enums.PublishStatusEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkPublishMapper;
import com.jingxuan.modules.log.service.LogService;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.publish.dto.FeaturedRequest;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublishServiceImpl - 发布服务")
class PublishServiceImplTest {

    @Mock private WorkMapper workMapper;
    @Mock private WorkPublishMapper workPublishMapper;
    @Mock private NotificationService notificationService;
    @Mock private LogService logService;

    @Captor private ArgumentCaptor<WorkPublish> publishCaptor;

    private PublishServiceImpl publishService;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    private static final Long ADMIN_ID = 300L;
    private static final Long WORK_ID = 1L;

    @BeforeEach
    void setUp() {
        publishService = new PublishServiceImpl(workMapper, workPublishMapper, notificationService, logService);
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::requireCurrentUserId).thenReturn(ADMIN_ID);
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
        work.setStatus(status);
        work.setSubmitterId(submitterId);
        return work;
    }

    private WorkPublish createPublish(Long workId, int status) {
        WorkPublish p = new WorkPublish();
        p.setId(workId);
        p.setWorkId(workId);
        p.setPublishStatus(status);
        p.setFeatured(0);
        return p;
    }

    @Nested
    @DisplayName("初始化发布")
    class InitPublish {

        @Test
        @DisplayName("成功初始化发布记录")
        void shouldInitPublish() {
            when(workPublishMapper.selectByWorkId(WORK_ID)).thenReturn(null);

            publishService.initPublish(WORK_ID);

            verify(workPublishMapper).insert(publishCaptor.capture());
            assertEquals(PublishStatusEnum.UNPUBLISHED.getValue(), publishCaptor.getValue().getPublishStatus());
            assertEquals(WORK_ID, publishCaptor.getValue().getWorkId());
        }

        @Test
        @DisplayName("已存在发布记录时不重复创建")
        void shouldSkipWhenExists() {
            when(workPublishMapper.selectByWorkId(WORK_ID)).thenReturn(new WorkPublish());

            publishService.initPublish(WORK_ID);

            verify(workPublishMapper, never()).insert(any(WorkPublish.class));
        }
    }

    @Nested
    @DisplayName("发布作品")
    class PublishWork {

        @Test
        @DisplayName("成功发布作品")
        void shouldPublishWork() {
            Work work = createWork(WORK_ID, AuditStatusEnum.APPROVED.getValue(), 100L);
            when(workMapper.selectById(WORK_ID)).thenReturn(work);

            WorkPublish publish = createPublish(WORK_ID, PublishStatusEnum.UNPUBLISHED.getValue());
            when(workPublishMapper.selectByWorkId(WORK_ID)).thenReturn(publish);

            publishService.publishWork(WORK_ID);

            verify(workPublishMapper).updateById(publishCaptor.capture());
            assertEquals(PublishStatusEnum.PUBLISHED.getValue(), publishCaptor.getValue().getPublishStatus());
            assertNotNull(publishCaptor.getValue().getPublishTime());
            assertEquals(ADMIN_ID, publishCaptor.getValue().getPublisherId());

            verify(notificationService).sendNotification(eq(100L), anyString(), anyString(), eq("publish"), eq(WORK_ID));
            verify(logService).recordAction("发布作品", "作品", WORK_ID);
        }

        @Test
        @DisplayName("作品不存在抛异常")
        void shouldThrowWhenWorkNotFound() {
            when(workMapper.selectById(WORK_ID)).thenReturn(null);

            assertThrows(BusinessException.class, () -> publishService.publishWork(WORK_ID));
        }

        @Test
        @DisplayName("未审核通过不可发布")
        void shouldThrowWhenNotApproved() {
            int[] invalidStatuses = {0, 1, 2};
            for (int status : invalidStatuses) {
                Work work = createWork(WORK_ID, status, 100L);
                when(workMapper.selectById(WORK_ID)).thenReturn(work);

                assertThrows(BusinessException.class,
                        () -> publishService.publishWork(WORK_ID),
                        "状态 " + status + " 不可发布");
            }
        }

        @Test
        @DisplayName("发布记录不存在抛异常")
        void shouldThrowWhenNoPublishRecord() {
            Work work = createWork(WORK_ID, AuditStatusEnum.APPROVED.getValue(), 100L);
            when(workMapper.selectById(WORK_ID)).thenReturn(work);
            when(workPublishMapper.selectByWorkId(WORK_ID)).thenReturn(null);

            assertThrows(BusinessException.class, () -> publishService.publishWork(WORK_ID));
        }
    }

    @Nested
    @DisplayName("下线作品")
    class OfflineWork {

        @Test
        @DisplayName("成功下线作品")
        void shouldOfflineWork() {
            WorkPublish publish = createPublish(WORK_ID, PublishStatusEnum.PUBLISHED.getValue());
            when(workPublishMapper.selectByWorkId(WORK_ID)).thenReturn(publish);
            when(workMapper.selectById(WORK_ID)).thenReturn(createWork(WORK_ID, 3, 100L));

            publishService.offlineWork(WORK_ID);

            verify(workPublishMapper).updateById(publishCaptor.capture());
            assertEquals(PublishStatusEnum.OFFLINE.getValue(), publishCaptor.getValue().getPublishStatus());
            assertNotNull(publishCaptor.getValue().getOfflineTime());
            verify(logService).recordAction("作品下线", "作品", WORK_ID);
        }

        @Test
        @DisplayName("发布记录不存在抛异常")
        void shouldThrowWhenNoRecord() {
            when(workPublishMapper.selectByWorkId(WORK_ID)).thenReturn(null);

            assertThrows(BusinessException.class, () -> publishService.offlineWork(WORK_ID));
        }
    }

    @Nested
    @DisplayName("精选设置")
    class SetFeatured {

        @Test
        @DisplayName("成功设为精选")
        void shouldSetFeatured() {
            when(workPublishMapper.selectCount(any())).thenReturn(1L);
            WorkPublish publish = createPublish(WORK_ID, PublishStatusEnum.PUBLISHED.getValue());
            when(workPublishMapper.selectByWorkId(WORK_ID)).thenReturn(publish);

            FeaturedRequest request = new FeaturedRequest();
            request.setWorkId(WORK_ID);
            request.setFeatured(CommonConstants.FEATURED_YES);

            publishService.setFeatured(request);

            verify(workPublishMapper).updateById(publishCaptor.capture());
            assertEquals(CommonConstants.FEATURED_YES, publishCaptor.getValue().getFeatured());
        }

        @Test
        @DisplayName("精选达到上限抛异常")
        void shouldThrowWhenMaxFeaturedReached() {
            when(workPublishMapper.selectCount(any())).thenReturn((long) CommonConstants.MAX_FEATURED_COUNT);

            FeaturedRequest request = new FeaturedRequest();
            request.setWorkId(WORK_ID);
            request.setFeatured(CommonConstants.FEATURED_YES);

            assertThrows(BusinessException.class, () -> publishService.setFeatured(request));
        }

        @Test
        @DisplayName("发布记录不存在抛异常")
        void shouldThrowWhenNoPublishRecord() {
            when(workPublishMapper.selectCount(any())).thenReturn(1L);
            when(workPublishMapper.selectByWorkId(WORK_ID)).thenReturn(null);

            FeaturedRequest request = new FeaturedRequest();
            request.setWorkId(WORK_ID);
            request.setFeatured(CommonConstants.FEATURED_YES);

            assertThrows(BusinessException.class, () -> publishService.setFeatured(request));
        }
    }
}

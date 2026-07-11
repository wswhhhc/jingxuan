package com.jingxuan.modules.notification.service.impl;

import com.jingxuan.entity.SysNotification;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysNotificationMapper;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentMatchers;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl - 通知服务")
class NotificationServiceImplTest {

    @Mock private SysNotificationMapper sysNotificationMapper;

    @Captor private ArgumentCaptor<SysNotification> notificationCaptor;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl();
        ReflectionTestUtils.setField(notificationService, "baseMapper", sysNotificationMapper);
    }

    @Nested
    @DisplayName("发送通知")
    class SendNotification {

        @Test
        @DisplayName("成功发送通知")
        void shouldSendNotification() {
            notificationService.sendNotification(100L, "标题", "内容", "audit", 1L);

            verify(sysNotificationMapper).insert(notificationCaptor.capture());
            assertEquals(100L, notificationCaptor.getValue().getUserId());
            assertEquals("标题", notificationCaptor.getValue().getTitle());
            assertEquals(0, notificationCaptor.getValue().getIsRead().intValue());
        }

        @Test
        @DisplayName("批量发送通知")
        void shouldSendBatchNotification() {
            List<Long> userIds = List.of(100L, 101L, 102L);

            notificationService.sendBatchNotification(userIds, "标题", "内容", "score", 1L);

            verify(sysNotificationMapper, times(3)).insert(any(SysNotification.class));
        }
    }

    @Nested
    @DisplayName("标记已读")
    class MarkAsRead {

        @Test
        @DisplayName("成功标记通知为已读")
        void shouldMarkAsRead() {
            SysNotification notification = new SysNotification();
            notification.setId(1L);
            notification.setUserId(100L);
            notification.setIsRead(0);
            when(sysNotificationMapper.selectById(1L)).thenReturn(notification);

            notificationService.markAsRead(1L, 100L);

            verify(sysNotificationMapper).updateById(ArgumentMatchers.<SysNotification>argThat(n ->
                    n.getIsRead() == 1 && n.getReadTime() != null
            ));
        }

        @Test
        @DisplayName("通知不存在抛异常")
        void shouldThrowWhenNotFound() {
            when(sysNotificationMapper.selectById(anyLong())).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> notificationService.markAsRead(1L, 100L));
        }

        @Test
        @DisplayName("无权操作他人通知抛异常")
        void shouldThrowWhenNotOwner() {
            SysNotification notification = new SysNotification();
            notification.setId(1L);
            notification.setUserId(200L);
            when(sysNotificationMapper.selectById(1L)).thenReturn(notification);

            assertThrows(BusinessException.class,
                    () -> notificationService.markAsRead(1L, 100L));
        }
    }

    @Nested
    @DisplayName("统计未读")
    class CountUnread {

        @Test
        @DisplayName("统计未读通知数")
        void shouldCountUnread() {
            when(sysNotificationMapper.selectCount(any())).thenReturn(3L);

            long count = notificationService.countUnread(100L);

            assertEquals(3L, count);
        }
    }
}

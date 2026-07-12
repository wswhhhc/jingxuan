package com.jingxuan.communication.internal.application;

import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SysNotification;
import com.jingxuan.modules.notification.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationQueryServiceTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final NotificationQueryService queryService = new NotificationQueryService(notificationService);

    @Test
    void queriesUserNotifications() {
        SysNotification n = new SysNotification(); n.setId(1L); n.setTitle("通知");
        when(notificationService.queryUserNotifications(7L, 1, 10, null))
                .thenReturn(new PageResult<>(List.of(n), 1L, 1L, 10L));
        var page = queryService.queryNotifications(7L, 1, 10, null);
        assertEquals(1, page.items().size());
    }

    @Test
    void marksAsRead() {
        queryService.markAsRead("1", 7L);
        verify(notificationService).markAsRead(1L, 7L);
    }

    @Test
    void countsUnread() {
        when(notificationService.countUnread(7L)).thenReturn(3L);
        assertEquals(3, queryService.countUnread(7L));
    }
}

package com.jingxuan.communication.internal.application;

import com.jingxuan.api.V1Ids;
import com.jingxuan.api.V1Page;
import com.jingxuan.api.V1PageInfo;
import com.jingxuan.communication.api.V1Notification;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SysNotification;
import com.jingxuan.modules.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;

@Service
public class NotificationQueryService {

    private final NotificationService notificationService;

    public NotificationQueryService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public V1Page<V1Notification> queryNotifications(Long userId, int page, int size, Boolean unreadOnly) {
        PageResult<SysNotification> result = notificationService.queryUserNotifications(userId, page, size, unreadOnly);
        return new V1Page<>(
            result.getRecords().stream().map(this::toV1Notification).toList(),
            V1PageInfo.of(page, size, result.getTotal())
        );
    }

    private V1Notification toV1Notification(SysNotification entity) {
        return new V1Notification(
            String.valueOf(entity.getId()), entity.getTitle(), entity.getContent(), entity.getType(),
            entity.getRefId() == null ? null : String.valueOf(entity.getRefId()),
            Integer.valueOf(1).equals(entity.getIsRead()),
            entity.getReadTime() == null ? null : entity.getReadTime().atOffset(ZoneOffset.ofHours(8)),
            entity.getCreateTime() == null ? null : entity.getCreateTime().atOffset(ZoneOffset.ofHours(8))
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(String notificationId, Long userId) {
        notificationService.markAsRead(V1Ids.parse(notificationId, "id"), userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        notificationService.markAllAsRead(userId);
    }

    public long countUnread(Long userId) {
        return notificationService.countUnread(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRead(Long userId) {
        notificationService.deleteRead(userId);
    }
}

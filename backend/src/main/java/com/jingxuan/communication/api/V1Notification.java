package com.jingxuan.communication.api;

import com.jingxuan.entity.SysNotification;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Schema(name = "V1Notification", description = "个人通知")
public record V1Notification(
    String id,
    String title,
    String content,
    String type,
    String refId,
    boolean isRead,
    OffsetDateTime readTime,
    OffsetDateTime createdAt
) {
    public static V1Notification from(SysNotification entity) {
        return new V1Notification(
            String.valueOf(entity.getId()),
            entity.getTitle(),
            entity.getContent(),
            entity.getType(),
            entity.getRefId() == null ? null : String.valueOf(entity.getRefId()),
            Integer.valueOf(1).equals(entity.getIsRead()),
            entity.getReadTime() == null ? null : entity.getReadTime().atOffset(ZoneOffset.ofHours(8)),
            entity.getCreateTime() == null ? null : entity.getCreateTime().atOffset(ZoneOffset.ofHours(8))
        );
    }
}

package com.jingxuan.communication.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

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
}

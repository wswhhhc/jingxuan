package com.jingxuan.communication.api;

import com.jingxuan.entity.SysNotice;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Schema(name = "V1Notice", description = "公告")
public record V1Notice(
    String id,
    String title,
    String content,
    String publisherName,
    OffsetDateTime publishTime,
    boolean topFlag,
    String status,
    String targetScope,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static V1Notice from(SysNotice entity) {
        return new V1Notice(
            String.valueOf(entity.getId()),
            entity.getTitle(),
            entity.getContent(),
            entity.getPublisherName(),
            entity.getPublishTime() == null ? null : entity.getPublishTime().atOffset(ZoneOffset.ofHours(8)),
            Integer.valueOf(1).equals(entity.getTopFlag()),
            entity.getStatus() == null || entity.getStatus() == 0 ? "DRAFT" : "PUBLISHED",
            entity.getTargetScope() == null ? "all" : entity.getTargetScope(),
            entity.getCreateTime() == null ? null : entity.getCreateTime().atOffset(ZoneOffset.ofHours(8)),
            entity.getUpdateTime() == null ? null : entity.getUpdateTime().atOffset(ZoneOffset.ofHours(8))
        );
    }
}

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
}

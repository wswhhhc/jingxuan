package com.jingxuan.campaign.api;

import com.jingxuan.entity.ScoreBatch;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** v1 批次详情读取模型（含待办要求与公示状态）。 */
public record V1BatchDetail(
        String id,
        String name,
        String status,
        String classScopes,
        String startAt,
        String endAt,
        Boolean rankPublished,
        String noticeTitle,
        String noticeContent,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static V1BatchDetail from(ScoreBatch source) {
        return new V1BatchDetail(
                source.getId().toString(),
                source.getBatchName(),
                status(source.getStatus()),
                source.getClassScopes(),
                at(source.getStartTime()),
                at(source.getEndTime()),
                Integer.valueOf(1).equals(source.getRankPublished()),
                source.getNoticeTitle(),
                source.getNoticeContent(),
                atOffset(source.getCreateTime()),
                atOffset(source.getUpdateTime())
        );
    }

    private static String at(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.ofHours(8)).toString();
    }

    private static OffsetDateTime atOffset(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.ofHours(8));
    }

    private static String status(Integer status) {
        return switch (status == null ? 0 : status) {
            case 1 -> "ACTIVE";
            case 2 -> "ENDED";
            default -> "DRAFT";
        };
    }
}

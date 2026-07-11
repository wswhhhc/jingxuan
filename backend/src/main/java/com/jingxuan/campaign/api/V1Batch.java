package com.jingxuan.campaign.api;

import com.jingxuan.entity.ScoreBatch;

import java.time.ZoneOffset;

/** v1 评分批次读取模型。 */
public record V1Batch(String id, String name, String startAt, String endAt, String status, boolean rankPublished) {
    public static V1Batch from(ScoreBatch source) {
        return new V1Batch(source.getId().toString(), source.getBatchName(), at(source.getStartTime()),
                at(source.getEndTime()), status(source.getStatus()), Integer.valueOf(1).equals(source.getRankPublished()));
    }

    private static String at(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.ofHours(8)).toString();
    }

    private static String status(Integer status) {
        return switch (status == null ? 0 : status) {
            case 1 -> "ACTIVE";
            case 2 -> "ENDED";
            default -> "DRAFT";
        };
    }
}

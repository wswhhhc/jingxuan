package com.jingxuan.campaign.api;

import com.jingxuan.entity.StudentTask;

/** v1 学生待办读取模型。 */
public record V1Task(String id, String batchId, String workId, String title, String content,
                     String status, String batchName, String endAt) {
    public static V1Task from(StudentTask source) {
        return new V1Task(source.getId().toString(), source.getBatchId().toString(), asString(source.getWorkId()),
                source.getTitle(), source.getContent(), status(source.getStatus()), source.getBatchName(), source.getEndTime());
    }

    private static String asString(Long value) { return value == null ? null : value.toString(); }

    private static String status(Integer value) {
        return switch (value == null ? 0 : value) {
            case 1 -> "COMPLETED";
            case 2 -> "REJECTED";
            case 3 -> "EXPIRED";
            default -> "PENDING";
        };
    }
}

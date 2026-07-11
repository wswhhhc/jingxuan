package com.jingxuan.portfolio.api;

import com.jingxuan.modules.work.dto.WorkListVO;
import java.time.ZoneOffset;

/** v1 作品列表项。 */
public record V1WorkSummary(String id, String title, String summary, String status, String submitterId,
                            String submitterName, String batchId, String submittedAt, java.util.List<String> tags) {
    public static V1WorkSummary from(WorkListVO value) {
        return new V1WorkSummary(s(value.getId()), value.getTitle(), value.getSummary(), status(value.getStatus()),
                s(value.getSubmitterId()), value.getSubmitterName(), s(value.getBatchId()),
                value.getSubmitTime() == null ? null : value.getSubmitTime().atOffset(ZoneOffset.ofHours(8)).toString(), value.getTags());
    }
    private static String s(Long value) { return value == null ? null : value.toString(); }
    private static String status(Integer value) { return switch (value == null ? 0 : value) { case 1 -> "SUBMITTED"; case 2 -> "REJECTED"; case 3 -> "APPROVED"; default -> "DRAFT"; }; }
}

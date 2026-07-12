package com.jingxuan.portfolio.api;

import com.jingxuan.modules.work.dto.WorkListVO;
import java.time.ZoneOffset;

/** v1 作品列表项。 */
public record V1WorkSummary(String id, String title, String summary, String status, String submitterId,
                            String submitterName, String batchId, String submittedAt, java.util.List<String> tags) {
    public static V1WorkSummary from(WorkListVO vo) {
        String statusStr = switch (vo.getStatus() == null ? 0 : vo.getStatus()) {
            case 1 -> "SUBMITTED";
            case 2 -> "REJECTED";
            case 3 -> "APPROVED";
            default -> "DRAFT";
        };
        return new V1WorkSummary(
            id(vo.getId()), vo.getTitle(), vo.getSummary(), statusStr,
            id(vo.getSubmitterId()), vo.getSubmitterName(),
            id(vo.getBatchId()),
            vo.getSubmitTime() == null ? null : vo.getSubmitTime().atOffset(ZoneOffset.ofHours(8)).toString(),
            java.util.List.of()
        );
    }
    private static String id(Long value) { return value == null ? null : value.toString(); }
}

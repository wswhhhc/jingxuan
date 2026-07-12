package com.jingxuan.portfolio.api;

import com.jingxuan.modules.work.dto.WorkDetailVO;

import java.time.ZoneOffset;
import java.util.List;

/** v1 作品详情。ID 始终作为不透明字符串输出。 */
public record V1WorkDetail(
        String id, String title, String summary, String techStack, String advisor,
        String coverUrl, String videoUrl, String previewUrl, String runDescription,
        String status, String submitterId, String submitterName, String submittedAt,
        String batchId, List<V1WorkMember> members, List<V1WorkAttachment> attachments,
        String publicationStatus, boolean featured, String averageScore, Integer rank,
        Integer likeCount, Integer viewCount, Boolean liked, List<String> tags) {

    private static String id(Long value) { return value == null ? null : value.toString(); }

    private static String status(Integer value) {
        return switch (value == null ? 0 : value) {
            case 1 -> "SUBMITTED";
            case 2 -> "REJECTED";
            case 3 -> "APPROVED";
            default -> "DRAFT";
        };
    }

    private static String publicationStatus(Integer value) {
        return switch (value == null ? 0 : value) {
            case 1 -> "PUBLISHED";
            case 2 -> "OFFLINE";
            default -> "UNPUBLISHED";
        };
    }
}

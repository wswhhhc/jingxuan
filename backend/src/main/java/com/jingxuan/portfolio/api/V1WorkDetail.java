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

    public static V1WorkDetail from(WorkDetailVO value) {
        return new V1WorkDetail(
                id(value.getId()), value.getTitle(), value.getSummary(), value.getTechStack(), value.getAdvisor(),
                value.getCoverUrl(), value.getVideoUrl(), value.getPreviewUrl(), value.getRunDesc(),
                status(value.getStatus()), id(value.getSubmitterId()), value.getSubmitterName(),
                value.getSubmitTime() == null ? null : value.getSubmitTime().atOffset(ZoneOffset.ofHours(8)).toString(),
                id(value.getBatchId()), members(value), attachments(value), publicationStatus(value.getPublishStatus()),
                Integer.valueOf(1).equals(value.getFeatured()), value.getAvgScore(), value.getRank(),
                value.getLikeCount(), value.getViewCount(), value.getLiked(), value.getTags());
    }

    /** 公开展廊不输出成员的学号、班级、用户 ID 或所属评分批次。 */
    public static V1WorkDetail publicFrom(WorkDetailVO value) {
        V1WorkDetail detail = from(value);
        List<V1WorkMember> publicMembers = value.getMembers() == null ? List.of()
                : value.getMembers().stream().map(V1WorkMember::publicFrom).toList();
        return new V1WorkDetail(detail.id(), detail.title(), detail.summary(), detail.techStack(), detail.advisor(),
                detail.coverUrl(), detail.videoUrl(), detail.previewUrl(), detail.runDescription(), detail.status(),
                null, detail.submitterName(), detail.submittedAt(), null, publicMembers, detail.attachments(),
                detail.publicationStatus(), detail.featured(), detail.averageScore(), detail.rank(), detail.likeCount(),
                detail.viewCount(), detail.liked(), detail.tags());
    }

    private static List<V1WorkMember> members(WorkDetailVO value) {
        return value.getMembers() == null ? List.of() : value.getMembers().stream().map(V1WorkMember::from).toList();
    }

    private static List<V1WorkAttachment> attachments(WorkDetailVO value) {
        return value.getAttachments() == null ? List.of() : value.getAttachments().stream().map(V1WorkAttachment::from).toList();
    }

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

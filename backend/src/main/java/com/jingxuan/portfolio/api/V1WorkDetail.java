package com.jingxuan.portfolio.api;

import com.jingxuan.modules.work.dto.WorkDetailVO;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/** v1 作品详情。ID 始终作为不透明字符串输出。 */
public record V1WorkDetail(
        String id, String title, String summary, String techStack, String advisor,
        String coverUrl, String videoUrl, String previewUrl, String runDescription,
        String status, String submitterId, String submitterName, String submittedAt,
        String batchId, List<V1WorkMember> members, List<V1WorkAttachment> attachments,
        String publicationStatus, boolean featured, String averageScore, Integer rank,
        Integer likeCount, Integer viewCount, Boolean liked, List<String> tags) {

    public static V1WorkDetail publicFrom(WorkDetailVO vo) {
        return new V1WorkDetail(
            id(vo.getId()), vo.getTitle(), vo.getSummary(), vo.getTechStack(), vo.getAdvisor(),
            vo.getCoverUrl(), vo.getVideoUrl(), vo.getPreviewUrl(), vo.getRunDesc(),
            status(vo.getStatus()), id(vo.getSubmitterId()), vo.getSubmitterName(),
            vo.getSubmitTime() == null ? null : vo.getSubmitTime().atOffset(ZoneOffset.ofHours(8)).toString(),
            id(vo.getBatchId()),
            vo.getMembers() == null ? List.of() : vo.getMembers().stream().map(V1WorkMember::from).toList(),
            vo.getAttachments() == null ? List.of() : vo.getAttachments().stream().map(V1WorkAttachment::from).toList(),
            publicationStatus(vo.getPublishStatus()), Integer.valueOf(1).equals(vo.getFeatured()),
            vo.getAvgScore(),
            vo.getRank(), vo.getLikeCount(), vo.getViewCount(), vo.getLiked(),
            vo.getTags() == null ? List.of() : vo.getTags()
        );
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

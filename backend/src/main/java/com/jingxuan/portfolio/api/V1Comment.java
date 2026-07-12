package com.jingxuan.portfolio.api;

import com.jingxuan.modules.comment.dto.CommentVO;
import java.time.ZoneOffset;

/** v1 公开评论，所有标识符均为字符串。 */
public record V1Comment(String id, String workId, String userId, String guestName, String userName,
                        String avatarUrl, String roleName, String content, String parentId,
                        String replyToUserName, String createdAt) {
    public static V1Comment from(CommentVO value) {
        return new V1Comment(id(value.getId()), id(value.getWorkId()), id(value.getUserId()), value.getGuestName(),
                value.getUserName(), value.getAvatarUrl(), value.getRoleName(), value.getContent(),
                id(value.getParentId()), value.getReplyToUserName(), value.getCreateTime() == null ? null
                : value.getCreateTime().atOffset(ZoneOffset.ofHours(8)).toString());
    }
    private static String id(Long id) { return id == null ? null : id.toString(); }
}

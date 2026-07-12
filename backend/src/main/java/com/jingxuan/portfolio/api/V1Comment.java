package com.jingxuan.portfolio.api;

import com.jingxuan.modules.comment.dto.CommentVO;
import java.time.ZoneOffset;

/** v1 公开评论，所有标识符均为字符串。 */
public record V1Comment(String id, String workId, String userId, String guestName, String userName,
                        String avatarUrl, String roleName, String content, String parentId,
                        String replyToUserName, String createdAt) {
}

package com.jingxuan.modules.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "评论VO")
public class CommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "作品ID")
    private Long workId;

    @Schema(description = "评论用户ID（游客为null）")
    private Long userId;

    @Schema(description = "游客昵称")
    private String guestName;

    @Schema(description = "评论者姓名")
    private String userName;

    @Schema(description = "评论者头像URL")
    private String avatarUrl;

    @Schema(description = "评论者角色")
    private String roleName;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "父评论ID")
    private Long parentId;

    @Schema(description = "被回复用户姓名")
    private String replyToUserName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子回复列表")
    private List<CommentVO> replies;
}

package com.jingxuan.modules.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "管理端评论列表 VO")
public class AdminCommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "作品ID")
    private Long workId;

    @Schema(description = "作品标题")
    private String workTitle;

    @Schema(description = "评论用户ID（游客为null）")
    private Long userId;

    @Schema(description = "游客昵称")
    private String guestName;

    @Schema(description = "评论者姓名")
    private String userName;

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
}

package com.jingxuan.portfolio.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Page;
import com.jingxuan.api.V1PageInfo;
import com.jingxuan.modules.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;

/** v1 管理端评论列表。 */
@V1Api
@RestController
@RequestMapping("/api/v1/works/comments")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "v1 评论管理")
public class V1CommentAdminController {

    private final CommentService commentService;

    public V1CommentAdminController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "管理端评论分页列表")
    public V1Page<V1AdminComment> listComments(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(required = false) Long workId,
            @RequestParam(required = false) String userKeyword,
            @RequestParam(required = false) String contentKeyword) {
        var result = commentService.getAdminComments(page, size, workId, userKeyword, contentKeyword);
        var items = result.getRecords().stream().map(V1AdminComment::from).toList();
        return new V1Page<>(items, V1PageInfo.of(page, size, result.getTotal()));
    }

    /** 管理端评论 v1 DTO。 */
    public record V1AdminComment(String id, String workId, String workTitle,
                                  String userId, String guestName, String userName,
                                  String roleName, String content, String parentId,
                                  String replyToUserName, String createdAt) {
        static V1AdminComment from(com.jingxuan.modules.comment.dto.AdminCommentVO value) {
            return new V1AdminComment(
                    id(value.getId()), id(value.getWorkId()), value.getWorkTitle(),
                    id(value.getUserId()), value.getGuestName(), value.getUserName(),
                    value.getRoleName(), value.getContent(), id(value.getParentId()),
                    value.getReplyToUserName(),
                    value.getCreateTime() == null ? null
                            : value.getCreateTime().atOffset(ZoneOffset.ofHours(8)).toString());
        }
        private static String id(Long v) { return v == null ? null : v.toString(); }
    }
}

package com.jingxuan.modules.comment.controller;

import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.modules.comment.dto.CommentVO;
import com.jingxuan.modules.comment.service.CommentService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 作品评论接口
 */
@Tag(name = "作品评论")
@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "发表评论")
    @PostMapping("/add")
    public Result<Long> add(@RequestParam Long workId,
                            @RequestParam String content,
                            @RequestParam(required = false) Long parentId,
                            @RequestParam(required = false) String guestName) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long commentId = commentService.addComment(workId, userId, content, parentId, guestName);
        return Result.ok(commentId);
    }

    @Operation(summary = "获取作品评论列表（含评论者信息）")
    @GetMapping("/list/{workId}")
    public Result<PageResult<CommentVO>> list(@PathVariable Long workId,
                                              @RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<CommentVO> pageResult = commentService.getWorkCommentsWithUserInfo(workId, pageNum, pageSize);
        return Result.ok(pageResult);
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{commentId}")
    public Result<Void> delete(@PathVariable Long commentId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        String roleCode = SecurityUtils.getCurrentRoleCode();
        commentService.deleteComment(commentId, userId, roleCode);
        return Result.ok();
    }
}

package com.jingxuan.modules.comment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.WorkComment;
import com.jingxuan.modules.comment.dto.AdminCommentVO;
import com.jingxuan.modules.comment.dto.CommentVO;

public interface CommentService extends IService<WorkComment> {

    /**
     * 发表评论
     */
    Long addComment(Long workId, Long userId, String content, Long parentId, String guestName);

    /**
     * 删除评论（包含其所有子回复）
     */
    void deleteComment(Long commentId, Long operatorId, String operatorRoleCode);

    /**
     * 获取作品评论列表（基础）
     */
    PageResult<WorkComment> getWorkComments(Long workId, int pageNum, int pageSize);

    /**
     * 获取作品评论列表（富化，含评论者姓名和角色）
     */
    PageResult<CommentVO> getWorkCommentsWithUserInfo(Long workId, int pageNum, int pageSize);

    /**
     * 管理端分页查询评论列表
     */
    PageResult<AdminCommentVO> getAdminComments(int pageNum, int pageSize, Long workId, String userKeyword, String contentKeyword);
}

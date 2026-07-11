package com.jingxuan.modules.work.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.Work;
import com.jingxuan.modules.work.dto.*;

import java.util.List;

public interface WorkService extends IService<Work> {

    /**
     * 创建作品（草稿状态）
     */
    Long createWork(WorkRequest request);

    /**
     * 编辑作品
     */
    void updateWork(Long id, WorkRequest request);

    /**
     * 提交审核（草稿→已提交）
     */
    void submitWork(Long id);

    /**
     * 删除作品（仅草稿或已驳回状态可删）
     */
    void deleteWork(Long id);

    /**
     * 管理员删除作品（不限制状态，清理所有关联数据）
     */
    void adminDeleteWork(Long workId);

    /**
     * 分页查询作品列表
     */
    PageResult<WorkListVO> queryWorkList(WorkQueryRequest request);

    /**
     * 获取作品详情
     */
    WorkDetailVO getWorkDetail(Long id);

    /**
     * 获取已审核通过的作品详情
     */
    WorkDetailVO getApprovedWorkDetail(Long id);

    /**
     * 获取当前学生自己的作品详情
     */
    WorkDetailVO getCurrentStudentWorkDetail(Long id);

    /**
     * 获取我的作品列表
     */
    List<WorkListVO> getMyWorks(Long userId);

    /**
     * 获取当前用户参与的作品
     */
    List<Work> listParticipatedWorks(Long userId);
}

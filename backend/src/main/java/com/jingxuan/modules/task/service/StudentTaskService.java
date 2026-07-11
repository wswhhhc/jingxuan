package com.jingxuan.modules.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.StudentTask;

import java.util.List;

public interface StudentTaskService extends IService<StudentTask> {

    /**
     * 批量创建待办（给指定学生列表针对某个批次创建待办）
     */
    void batchCreateTasks(List<Long> userIds, Long batchId, String title, String content);

    /**
     * 获取当前学生的待办列表
     */
    List<StudentTask> getStudentTasks(Long userId);

    /**
     * 学生提交作品后回填 workId 并更新状态（待处理→已完成）
     */
    void completeTask(Long taskId, Long workId);

    /**
     * 作品被驳回时重置任务状态（已完成→已驳回）
     */
    void rejectTask(Long workId);

    /**
     * 管理员删除作品后重置待办（已完成/已驳回→待处理，清空 workId）
     */
    void resetTask(Long workId);

    /**
     * 根据 batchId + userId 获取待办
     */
    StudentTask getByUserAndBatch(Long userId, Long batchId);
}

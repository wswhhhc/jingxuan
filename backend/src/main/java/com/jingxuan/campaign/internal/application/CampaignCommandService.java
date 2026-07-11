package com.jingxuan.campaign.internal.application;

import com.jingxuan.entity.StudentTask;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.exception.NotFoundException;
import com.jingxuan.mapper.StudentTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 学生待办状态机命令用例。 */
@Service
public class CampaignCommandService {
    private final StudentTaskMapper taskMapper;
    public CampaignCommandService(StudentTaskMapper taskMapper) { this.taskMapper = taskMapper; }

    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long userId, Long taskId, Long workId) {
        StudentTask task = taskMapper.selectById(taskId);
        if (task == null || !userId.equals(task.getUserId())) throw new NotFoundException("待办不存在");
        int status = task.getStatus() == null ? 0 : task.getStatus();
        if (status != 0 && status != 2) throw new BusinessException(409, "当前待办状态不允许完成");
        task.setWorkId(workId);
        task.setStatus(1);
        taskMapper.updateById(task);
    }
}

package com.jingxuan.workflow;

import com.jingxuan.campaign.api.CampaignTaskCompletion;
import com.jingxuan.modules.work.dto.WorkRequest;
import com.jingxuan.modules.work.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 创建作品并完成指定待办的跨模块事务用例。 */
@Service
@RequiredArgsConstructor
public class TaskWorkSubmissionWorkflow {

    private final WorkService workService;
    private final CampaignTaskCompletion campaignTaskCompletion;

    @Transactional(rollbackFor = Exception.class)
    public Long createWorkForTask(Long userId, Long taskId, WorkRequest request) {
        Long workId = workService.createWork(request);
        campaignTaskCompletion.completeTask(userId, taskId, workId);
        return workId;
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeTaskForSubmittedWork(Long userId, Long taskId, Long workId) {
        workService.submitWork(workId);
        campaignTaskCompletion.completeTask(userId, taskId, workId);
    }
}

package com.jingxuan.campaign.api;

/** portfolio/workflow 完成学生待办时使用的稳定 campaign API。 */
public interface CampaignTaskCompletion {

    void completeTask(Long userId, Long taskId, Long workId);
}

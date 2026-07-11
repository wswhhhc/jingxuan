package com.jingxuan.modules.scorebatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.ScoreBatch;

import java.util.List;

public interface ScoreBatchService extends IService<ScoreBatch> {

    /**
     * 创建评分批次
     */
    Long createBatch(ScoreBatch scoreBatch);

    /**
     * 更新评分批次
     */
    void updateBatch(ScoreBatch scoreBatch);

    /**
     * 分页查询批次列表
     */
    PageResult<ScoreBatch> queryBatchList(int pageNum, int pageSize);

    /**
     * 获取当前进行中的批次
     */
    ScoreBatch getActiveBatch();

    /**
     * 检查是否在评分有效期内
     */
    boolean isWithinScoringPeriod(Long batchId);

    /**
     * 获取当前学生可参与的进行中批次（classScopes 匹配该学生班级）
     */
    List<ScoreBatch> getAvailableBatchesForStudent(Long userId);

    /**
     * 删除评分批次（逻辑删除）
     */
    void deleteBatch(Long id);

    /**
     * 公示排行榜
     */
    void publishRanking(Long batchId);

    /**
     * 取消排行榜公示
     */
    void unpublishRanking(Long batchId);

    /**
     * 检查排行榜是否已公示
     */
    boolean isRankPublished(Long batchId);

    /**
     * 保存批次待办要求内容
     */
    void saveNotice(Long batchId, String noticeTitle, String noticeContent);

    /**
     * 发布批次待办：给班级范围内所有学生创建待办任务
     * 如果已存在待办（重新发布），会另外发送通知提醒学生
     */
    void publishTask(Long batchId);
}

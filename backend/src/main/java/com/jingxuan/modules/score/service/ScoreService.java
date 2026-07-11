package com.jingxuan.modules.score.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.entity.WorkScore;
import com.jingxuan.modules.score.dto.ScoreSubmitRequest;
import com.jingxuan.modules.score.dto.ScoreSummaryVO;
import com.jingxuan.modules.score.dto.ScoreVO;

import java.util.List;

public interface ScoreService extends IService<WorkScore> {

    /**
     * 教师提交评分
     */
    void submitScore(Long teacherId, ScoreSubmitRequest request);

    /**
     * 获取教师对某作品的评分
     */
    ScoreVO getTeacherScore(Long workId, Long teacherId);

    /**
     * 获取作品的评分列表
     */
    List<ScoreVO> getWorkScores(Long workId);

    /**
     * 获取作品评分汇总
     */
    ScoreSummaryVO getScoreSummary(Long workId);

    /**
     * 获取某批次的评分汇总列表
     */
    List<ScoreSummaryVO> getBatchScoreSummary(Long batchId);
}

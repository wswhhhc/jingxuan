package com.jingxuan.evaluation.internal.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.api.V1Page;
import com.jingxuan.api.V1PageInfo;
import com.jingxuan.evaluation.api.V1ScoreEntry;
import com.jingxuan.evaluation.api.V1ScoreHistoryEntry;
import com.jingxuan.evaluation.api.V1ScoreSummary;
import com.jingxuan.evaluation.api.V1BatchScoreDetail;
import com.jingxuan.modules.adapter.AdminScoreFacade;
import com.jingxuan.modules.adapter.TeacherWorkFacade;
import com.jingxuan.modules.score.dto.ScoreSummaryVO;
import com.jingxuan.modules.score.dto.ScoreVO;
import com.jingxuan.modules.score.dto.AdminScoreDetailVO;
import com.jingxuan.modules.score.dto.TeacherScoreHistoryVO;
import com.jingxuan.modules.score.service.ScoreService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** 评分查询用例。 */
@Service
public class ScoreQueryService {

    private final ScoreService scoreService;
    private final TeacherWorkFacade teacherWorkFacade;
    private final AdminScoreFacade adminScoreFacade;

    public ScoreQueryService(ScoreService scoreService, TeacherWorkFacade teacherWorkFacade,
                             AdminScoreFacade adminScoreFacade) {
        this.scoreService = scoreService;
        this.teacherWorkFacade = teacherWorkFacade;
        this.adminScoreFacade = adminScoreFacade;
    }

    /** 获取作品评分列表。 */
    public List<V1ScoreEntry> getWorkScores(Long workId) {
        List<ScoreVO> scores = scoreService.getWorkScores(workId);
        return scores.stream().map(s -> new V1ScoreEntry(
                s.getId().toString(),
                s.getWorkId().toString(),
                s.getWorkTitle(),
                s.getTeacherId().toString(),
                s.getTeacherName(),
                s.getInnovation(),
                s.getDifficulty(),
                s.getCompletion(),
                s.getPracticality(),
                s.getTotal(),
                s.getComment(),
                s.getCreateTime() == null ? null : s.getCreateTime().toString()
        )).toList();
    }

    /** 获取作品评分汇总。 */
    public V1ScoreSummary getScoreSummary(Long workId) {
        ScoreSummaryVO summary = scoreService.getScoreSummary(workId);
        if (summary == null) return null;
        return new V1ScoreSummary(
                summary.getWorkId().toString(),
                summary.getWorkTitle(),
                summary.getAvgTotal(),
                summary.getAvgInnovation(),
                summary.getAvgDifficulty(),
                summary.getAvgCompletion(),
                summary.getAvgPracticality(),
                summary.getTeacherCount()
        );
    }

    /** 获取我的评分历史（分页）。 */
    public V1Page<V1ScoreHistoryEntry> getMyScoreHistory(Long teacherId, int page, int size) {
        var mpPage = teacherWorkFacade.queryScoreHistory(teacherId, page, size);
        List<V1ScoreHistoryEntry> items = mpPage.getRecords().stream()
                .map(s -> new V1ScoreHistoryEntry(
                        s.getId().toString(),
                        s.getWorkId().toString(),
                        s.getWorkTitle(),
                        s.getBatchId() == null ? null : s.getBatchId().toString(),
                        s.getInnovation(),
                        s.getDifficulty(),
                        s.getCompletion(),
                        s.getPracticality(),
                        s.getTotal(),
                        s.getComment(),
                        s.getScoreTime() == null ? null : s.getScoreTime().toString()
                )).toList();
        return new V1Page<>(items, V1PageInfo.of(page, size, mpPage.getTotal()));
    }

    /** 获取批次评分明细（管理员）。 */
    public List<V1BatchScoreDetail> getBatchScoreDetail(Long batchId) {
        return adminScoreFacade.getBatchScoreDetail(batchId).stream()
                .map(d -> new V1BatchScoreDetail(
                        d.getWorkId().toString(),
                        d.getWorkTitle(),
                        d.getSubmitterName(),
                        d.getScores().stream().map(t -> {
                            var item = new V1BatchScoreDetail.V1TeacherScoreItem(
                                    t.getTeacherName(),
                                    t.getInnovation(),
                                    t.getDifficulty(),
                                    t.getCompletion(),
                                    t.getPracticality(),
                                    t.getTotal(),
                                    t.getComment()
                            );
                            return item;
                        }).toList()
                )).toList();
    }
}

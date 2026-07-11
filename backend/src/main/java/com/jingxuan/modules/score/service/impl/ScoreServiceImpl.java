package com.jingxuan.modules.score.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkScore;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.util.ClassScopeUtil;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkScoreMapper;
import com.jingxuan.modules.score.dto.ScoreSubmitRequest;
import com.jingxuan.modules.score.dto.ScoreSummaryVO;
import com.jingxuan.modules.score.dto.ScoreVO;
import com.jingxuan.modules.score.service.ScoreService;
import com.jingxuan.modules.log.service.LogService;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.rank.service.RankService;
import com.jingxuan.modules.sensitive.service.DeepSeekReviewService;
import com.jingxuan.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreServiceImpl extends ServiceImpl<WorkScoreMapper, WorkScore> implements ScoreService {

    private final WorkScoreMapper workScoreMapper;
    private final WorkMapper workMapper;
    private final ScoreBatchMapper scoreBatchMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysDictMapper sysDictMapper;

    @Autowired
    private DeepSeekReviewService deepSeekReviewService;

    @Autowired
    private LogService logService;

    @Autowired
    private RankService rankService;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitScore(Long teacherId, ScoreSubmitRequest request) {
        // 1. 校验作品存在且已通过审核
        Work work = workMapper.selectById(request.getWorkId());
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        if (work.getStatus() != 3) {
            throw new BusinessException("作品尚未通过审核，无法评分");
        }

        // 2. 校验评分批次及有效期（如果作品关联了批次且批次存在才校验）
        if (work.getBatchId() != null) {
            ScoreBatch batch = scoreBatchMapper.selectById(work.getBatchId());
            if (batch != null) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(batch.getStartTime()) || now.isAfter(batch.getEndTime())) {
                    throw new BusinessException("当前不在评分有效期内");
                }
                // 校验班级范围
                if (StrUtil.isNotBlank(batch.getClassScopes())) {
                    SysUser submitter = sysUserMapper.selectById(work.getSubmitterId());
                    if (submitter != null && submitter.getClassId() != null) {
                        Set<String> scopes = ClassScopeUtil.parseToStringSet(batch.getClassScopes());
                        String classId = String.valueOf(submitter.getClassId());
                        SysDict classDict = sysDictMapper.selectById(submitter.getClassId());
                        String classValue = classDict != null ? classDict.getDictValue() : null;
                        boolean inScope = scopes.contains(classId)
                                || (StrUtil.isNotBlank(classValue) && scopes.contains(classValue));
                        if (!inScope) {
                            throw new BusinessException("该作品不在当前评分批次的班级范围内");
                        }
                    }
                }
            }
        }

        // 3. 对评语进行内容安全审核
        if (request.getComment() != null && !request.getComment().isBlank()) {
            DeepSeekReviewService.ReviewResult review = deepSeekReviewService.review(request.getComment(), "score");
            if (!review.isPassed()) {
                throw new BusinessException("评语内容违规：" + review.getReason());
            }
        }

        // 4. 计算总分（满分100）
        BigDecimal total = request.getInnovation()
                .add(request.getDifficulty())
                .add(request.getCompletion())
                .add(request.getPracticality());

        // 4. 原子化 Upsert：利用 MySQL ON DUPLICATE KEY UPDATE 避免并发竞态
        WorkScore score = new WorkScore();
        score.setWorkId(request.getWorkId());
        score.setTeacherId(teacherId);
        score.setBatchId(work.getBatchId());
        score.setInnovation(request.getInnovation());
        score.setDifficulty(request.getDifficulty());
        score.setCompletion(request.getCompletion());
        score.setPracticality(request.getPracticality());
        score.setTotal(total);
        score.setComment(request.getComment());
        if (score.getId() == null) {
            score.setId(IdWorker.getId());
        }
        workScoreMapper.upsert(
                score.getId(), score.getWorkId(), score.getTeacherId(),
                score.getBatchId(), score.getInnovation(), score.getDifficulty(),
                score.getCompletion(), score.getPracticality(), score.getTotal(),
                score.getComment()
        );

        logService.recordAction("提交评分", "作品", request.getWorkId());

        // 5. 发送通知给作品提交者
        try {
            notificationService.sendNotification(
                    work.getSubmitterId(),
                    "作品收到新评分",
                    "您的作品《" + work.getTitle() + "》收到教师评分，总分：" + total,
                    "score",
                    work.getId()
            );
        } catch (Exception e) {
            log.warn("发送评分通知失败", e);
        }

        // 6. 刷新排行榜缓存（自动触发）
        if (work.getBatchId() != null) {
            try {
                rankService.refreshRankCache(work.getBatchId());
            } catch (Exception e) {
                log.warn("刷新排行榜缓存失败", e);
            }
        }
    }

    @Override
    public ScoreVO getTeacherScore(Long workId, Long teacherId) {
        WorkScore score = workScoreMapper.selectByWorkAndTeacher(workId, teacherId);
        if (score == null) {
            return null;
        }
        ScoreVO vo = convertToScoreVO(score);

        // 查询教师姓名
        SysUser teacher = sysUserMapper.selectById(teacherId);
        if (teacher != null) {
            vo.setTeacherName(teacher.getRealName());
        }

        return vo;
    }

    @Override
    public List<ScoreVO> getWorkScores(Long workId) {
        List<WorkScore> scores = workScoreMapper.selectByWorkId(workId);
        List<ScoreVO> voList = new ArrayList<>();
        for (WorkScore score : scores) {
            ScoreVO vo = convertToScoreVO(score);
            // 查询该评分对应的教师姓名
            if (score.getTeacherId() != null) {
                SysUser teacher = sysUserMapper.selectById(score.getTeacherId());
                if (teacher != null) {
                    vo.setTeacherName(teacher.getRealName());
                }
            }
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public ScoreSummaryVO getScoreSummary(Long workId) {
        List<WorkScore> scores = workScoreMapper.selectByWorkId(workId);
        if (scores.isEmpty()) {
            return null;
        }

        Work work = workMapper.selectById(workId);

        ScoreSummaryVO summary = new ScoreSummaryVO();
        summary.setWorkId(workId);
        summary.setWorkTitle(work != null ? work.getTitle() : null);
        summary.setTeacherCount(scores.size());

        computeScoreAverages(scores, summary);
        return summary;
    }

    @Override
    public List<ScoreSummaryVO> getBatchScoreSummary(Long batchId) {
        LambdaQueryWrapper<WorkScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkScore::getBatchId, batchId)
                .eq(WorkScore::getDeleted, 0);
        List<WorkScore> allScores = workScoreMapper.selectList(wrapper);
        if (allScores.isEmpty()) {
            return new ArrayList<>();
        }

        // 按 workId 分组，在 Java 中计算各维度平均值
        Map<Long, List<WorkScore>> grouped = allScores.stream()
                .collect(Collectors.groupingBy(WorkScore::getWorkId));

        List<ScoreSummaryVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<WorkScore>> entry : grouped.entrySet()) {
            Long workId = entry.getKey();
            List<WorkScore> workScores = entry.getValue();

            Work work = workMapper.selectById(workId);

            ScoreSummaryVO summary = new ScoreSummaryVO();
            summary.setWorkId(workId);
            summary.setWorkTitle(work != null ? work.getTitle() : null);
            summary.setTeacherCount(workScores.size());

            computeScoreAverages(workScores, summary);

            result.add(summary);
        }

        return result;
    }

    /**
     * 将 WorkScore 实体转换为 ScoreVO（不含 teacherName）
     */
    

    private void computeScoreAverages(List<WorkScore> scores, ScoreSummaryVO summary) {
        BigDecimal sumInnovation = BigDecimal.ZERO;
        BigDecimal sumDifficulty = BigDecimal.ZERO;
        BigDecimal sumCompletion = BigDecimal.ZERO;
        BigDecimal sumPracticality = BigDecimal.ZERO;
        BigDecimal sumTotal = BigDecimal.ZERO;

        for (WorkScore s : scores) {
            sumInnovation = sumInnovation.add(s.getInnovation() != null ? s.getInnovation() : BigDecimal.ZERO);
            sumDifficulty = sumDifficulty.add(s.getDifficulty() != null ? s.getDifficulty() : BigDecimal.ZERO);
            sumCompletion = sumCompletion.add(s.getCompletion() != null ? s.getCompletion() : BigDecimal.ZERO);
            sumPracticality = sumPracticality.add(s.getPracticality() != null ? s.getPracticality() : BigDecimal.ZERO);
            sumTotal = sumTotal.add(s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO);
        }

        int count = scores.size();
        summary.setAvgInnovation(sumInnovation.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
        summary.setAvgDifficulty(sumDifficulty.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
        summary.setAvgCompletion(sumCompletion.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
        summary.setAvgPracticality(sumPracticality.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
        summary.setAvgTotal(sumTotal.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
    }

    private ScoreVO convertToScoreVO(WorkScore score) {
        ScoreVO vo = new ScoreVO();
        vo.setId(score.getId());
        vo.setWorkId(score.getWorkId());
        vo.setTeacherId(score.getTeacherId());
        vo.setInnovation(score.getInnovation());
        vo.setDifficulty(score.getDifficulty());
        vo.setCompletion(score.getCompletion());
        vo.setPracticality(score.getPracticality());
        vo.setTotal(score.getTotal());
        vo.setComment(score.getComment());
        vo.setCreateTime(score.getCreateTime());
        return vo;
    }
}

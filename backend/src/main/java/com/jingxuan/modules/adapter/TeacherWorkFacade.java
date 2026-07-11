package com.jingxuan.modules.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.json.JSONUtil;
import com.jingxuan.common.PageResult;
import com.jingxuan.util.ClassScopeUtil;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkScore;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkScoreMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.score.dto.TeacherScoreHistoryVO;
import com.jingxuan.modules.scorebatch.service.ScoreBatchService;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkListVO;
import com.jingxuan.modules.work.dto.WorkQueryRequest;
import com.jingxuan.modules.work.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherWorkFacade {

    private final WorkService workService;
    private final ScoreBatchService scoreBatchService;
    private final NotificationService notificationService;
    private final WorkScoreMapper workScoreMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDictMapper sysDictMapper;

    public PageResult<WorkListVO> queryScoredWorks(int page, int size, String keyword,
                                                   String techStack, Long batchId,
                                                   Boolean onlyUnscored, Long teacherId) {
        WorkQueryRequest request = new WorkQueryRequest();
        request.setPageNum(page);
        request.setPageSize(size);
        request.setKeyword(keyword);
        request.setTechStack(techStack);
        request.setBatchId(batchId);
        request.setStatus(3);

        Set<Long> scoredIds = getScoredWorkIds(teacherId);
        if (Boolean.TRUE.equals(onlyUnscored) && !scoredIds.isEmpty()) {
            request.setExcludeWorkIds(scoredIds.stream().toList());
        }
        PageResult<WorkListVO> result = workService.queryWorkList(request);
        result.getRecords().forEach(vo -> vo.setScored(scoredIds.contains(vo.getId())));
        return result;
    }

    public WorkDetailVO getAnonymousApprovedWorkDetail(Long id) {
        WorkDetailVO vo = workService.getApprovedWorkDetail(id);
        vo.setSubmitterName(null);
        vo.setSubmitterId(null);
        vo.setMembers(null);
        return vo;
    }

    public PageResult<TeacherScoreHistoryVO> queryScoreHistory(Long teacherId, int page, int size) {
        Page<WorkScore> scorePage = new Page<>(page, size);
        LambdaQueryWrapper<WorkScore> wrapper = new LambdaQueryWrapper<WorkScore>()
                .eq(WorkScore::getTeacherId, teacherId)
                .orderByDesc(WorkScore::getUpdateTime)
                .orderByDesc(WorkScore::getId);
        workScoreMapper.selectPage(scorePage, wrapper);

        List<WorkScore> records = scorePage.getRecords();
        Map<Long, String> workTitleMap = loadWorkTitleMap(records);
        List<TeacherScoreHistoryVO> voList = records.stream()
                .map(score -> toScoreHistoryVO(score, workTitleMap))
                .collect(Collectors.toList());

        return new PageResult<>(voList, scorePage.getTotal(),
                scorePage.getCurrent(), scorePage.getSize());
    }

    public Map<String, Object> getDashboardStats(Long teacherId) {
        List<Work> approvedWorks = workService.list(
                new LambdaQueryWrapper<Work>().eq(Work::getStatus, 3));

        Set<Long> scorableWorkIds = approvedWorks.stream()
                .filter(this::isWorkScorableForTeacher)
                .map(Work::getId)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> scoredWorkIds = getScoredWorkIds(teacherId).stream()
                .filter(scorableWorkIds::contains)
                .collect(Collectors.toSet());

        int totalScorableWorks = scorableWorkIds.size();
        int scoredWorks = scoredWorkIds.size();
        Map<String, Object> data = new HashMap<>();
        data.put("pendingWorks", Math.max(totalScorableWorks - scoredWorks, 0));
        data.put("scoredWorks", scoredWorks);
        data.put("totalScorableWorks", totalScorableWorks);
        data.put("completionRate", totalScorableWorks == 0
                ? 0
                : Math.round(scoredWorks * 100.0 / totalScorableWorks));
        data.put("activeBatchCount", countActiveBatches());
        data.put("unreadCount", notificationService.countUnread(teacherId));
        return data;
    }

    /**
     * 获取排行分类（技术栈），仅包含已公示批次的分类数据
     */
    public List<Map<String, String>> listRankingCategories(Long batchId) {
        // 如果指定了批次，检查是否已公示
        if (batchId != null) {
            ScoreBatch batch = scoreBatchService.getById(batchId);
            if (batch == null || !Integer.valueOf(1).equals(batch.getRankPublished())) {
                return Collections.emptyList();
            }
        }
        return workService.list().stream()
                .map(Work::getTechStack)
                .filter(Objects::nonNull)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .map(tech -> {
                    Map<String, String> item = new HashMap<>();
                    item.put("label", tech);
                    item.put("value", tech);
                    return item;
                })
                .collect(Collectors.toList());
    }

    private Set<Long> getScoredWorkIds(Long teacherId) {
        return workScoreMapper.selectList(
                        new LambdaQueryWrapper<WorkScore>().eq(WorkScore::getTeacherId, teacherId))
                .stream()
                .map(WorkScore::getWorkId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Long, String> loadWorkTitleMap(List<WorkScore> records) {
        if (records.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> workIds = records.stream()
                .map(WorkScore::getWorkId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (workIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return workService.listByIds(workIds).stream()
                .collect(Collectors.toMap(Work::getId, Work::getTitle));
    }

    private TeacherScoreHistoryVO toScoreHistoryVO(WorkScore score, Map<Long, String> workTitleMap) {
        TeacherScoreHistoryVO vo = new TeacherScoreHistoryVO();
        vo.setId(score.getId());
        vo.setWorkId(score.getWorkId());
        vo.setWorkTitle(workTitleMap.getOrDefault(score.getWorkId(), "作品#" + score.getWorkId()));
        vo.setBatchId(score.getBatchId());
        vo.setInnovation(score.getInnovation());
        vo.setDifficulty(score.getDifficulty());
        vo.setCompletion(score.getCompletion());
        vo.setPracticality(score.getPracticality());
        vo.setTotal(score.getTotal());
        vo.setComment(score.getComment());
        vo.setScoreTime(score.getUpdateTime() != null ? score.getUpdateTime() : score.getCreateTime());
        return vo;
    }

    private long countActiveBatches() {
        return scoreBatchService.list().stream()
                .filter(batch -> batch.getStatus() != null && batch.getStatus() == 1)
                .count();
    }

    private boolean isWorkScorableForTeacher(Work work) {
        if (work.getBatchId() == null) {
            return true;
        }
        ScoreBatch batch = scoreBatchService.getById(work.getBatchId());
        if (batch == null) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        if (batch.getStartTime() != null && now.isBefore(batch.getStartTime())) {
            return false;
        }
        if (batch.getEndTime() != null && now.isAfter(batch.getEndTime())) {
            return false;
        }
        if (batch.getClassScopes() == null || batch.getClassScopes().isBlank()) {
            return true;
        }
        SysUser submitter = sysUserMapper.selectById(work.getSubmitterId());
        if (submitter == null || submitter.getClassId() == null) {
            return true;
        }

        Set<String> scopes = ClassScopeUtil.parseToStringSet(batch.getClassScopes());
        String classId = String.valueOf(submitter.getClassId());
        SysDict classDict = sysDictMapper.selectById(submitter.getClassId());
        String classValue = classDict != null ? classDict.getDictValue() : null;
        return scopes.contains(classId)
                || (classValue != null && !classValue.isBlank() && scopes.contains(classValue));
    }
}

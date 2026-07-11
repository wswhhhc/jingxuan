package com.jingxuan.modules.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkPublish;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkPublishMapper;
import com.jingxuan.modules.scorebatch.service.ScoreBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardFacade {

    private final WorkMapper workMapper;
    private final WorkPublishMapper workPublishMapper;
    private final SysUserMapper sysUserMapper;
    private final ScoreBatchService scoreBatchService;

    public Map<String, Object> getStats() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalWorks", workMapper.selectCount(null));
        data.put("pendingAudit", workMapper.selectCount(
                new LambdaQueryWrapper<Work>().eq(Work::getStatus, 1)));
        data.put("publishedWorks", workPublishMapper.selectCount(
                new LambdaQueryWrapper<WorkPublish>().eq(WorkPublish::getPublishStatus, 1)));
        data.put("totalTeachers", sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, 2)));
        data.put("totalStudents", sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, 1)));
        data.put("activeBatches", countActiveBatches());
        data.put("recentWorks", getRecentWorks());
        data.put("scoreDistribution", List.of());
        return data;
    }

    public Map<String, Object> getChartData() {
        Map<String, Object> data = new HashMap<>();
        data.put("techStackDist", getTechStackDistribution());
        data.put("statusDist", getStatusDistribution());
        data.put("scoreDist", getScoreDistribution());
        return data;
    }

    private long countActiveBatches() {
        return scoreBatchService.list().stream()
                .filter(batch -> batch.getStatus() != null && batch.getStatus() == 1)
                .count();
    }

    private List<Map<String, Object>> getRecentWorks() {
        Page<Work> recentPage = new Page<>(1, 5);
        LambdaQueryWrapper<Work> recentWrapper = new LambdaQueryWrapper<Work>()
                .orderByDesc(Work::getSubmitTime);
        workMapper.selectPage(recentPage, recentWrapper);
        return recentPage.getRecords().stream()
                .map(this::toRecentWorkItem)
                .toList();
    }

    private Map<String, Object> toRecentWorkItem(Work work) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", work.getId());
        item.put("title", work.getTitle());
        item.put("techStack", work.getTechStack());
        item.put("status", work.getStatus());
        item.put("submitTime", work.getSubmitTime());
        SysUser user = sysUserMapper.selectById(work.getSubmitterId());
        item.put("submitterName", user != null ? user.getRealName() : null);
        return item;
    }

    private List<Map<String, Object>> getTechStackDistribution() {
        Map<String, Long> techCount = new HashMap<>();
        for (Work work : workMapper.selectList(null)) {
            if (work.getTechStack() == null || work.getTechStack().isEmpty()) {
                continue;
            }
            for (String tech : work.getTechStack().split(",")) {
                String normalized = tech.trim();
                if (!normalized.isEmpty()) {
                    techCount.merge(normalized, 1L, Long::sum);
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : techCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            result.add(item);
        }
        return result;
    }

    private Map<String, Long> getStatusDistribution() {
        Map<String, Long> statusDist = new LinkedHashMap<>();
        statusDist.put("草稿", 0L);
        statusDist.put("已提交", 0L);
        statusDist.put("已驳回", 0L);
        statusDist.put("已通过", 0L);
        statusDist.put("其他", 0L);
        for (Work work : workMapper.selectList(null)) {
            statusDist.merge(getStatusLabel(work.getStatus()), 1L, Long::sum);
        }
        return statusDist;
    }

    private String getStatusLabel(Integer status) {
        if (status == null) {
            return "其他";
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "已提交";
            case 2 -> "已驳回";
            case 3 -> "已通过";
            default -> "其他";
        };
    }

    private Object getScoreDistribution() {
        try {
            return workMapper.selectScoreDistribution();
        } catch (Exception e) {
            return List.of();
        }
    }
}

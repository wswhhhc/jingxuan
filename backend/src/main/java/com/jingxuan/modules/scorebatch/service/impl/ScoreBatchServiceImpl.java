package com.jingxuan.modules.scorebatch.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.common.PageResult;
import com.jingxuan.common.PageUtil;
import com.jingxuan.util.ClassScopeUtil;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.rank.service.RankService;
import com.jingxuan.modules.scorebatch.service.ScoreBatchService;
import com.jingxuan.modules.task.service.StudentTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评分批次 服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoreBatchServiceImpl extends ServiceImpl<ScoreBatchMapper, ScoreBatch> implements ScoreBatchService {

    private final WorkMapper workMapper;
    private final NotificationService notificationService;
    private final StudentTaskService studentTaskService;
    private final RankService rankService;
    private final SysUserMapper sysUserMapper;
    private final SysDictMapper sysDictMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBatch(ScoreBatch scoreBatch) {
        if (scoreBatch.getStatus() == null) {
            scoreBatch.setStatus(1);
        }
        if (scoreBatch.getRankPublished() == null) {
            scoreBatch.setRankPublished(0);
        }
        baseMapper.insert(scoreBatch);
        return scoreBatch.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatch(ScoreBatch scoreBatch) {
        baseMapper.updateById(scoreBatch);
    }

    @Override
    public PageResult<ScoreBatch> queryBatchList(int pageNum, int pageSize) {
        return PageUtil.query(pageNum, pageSize, baseMapper,
                w -> w.orderByDesc(ScoreBatch::getCreateTime));
    }

    @Override
    public ScoreBatch getActiveBatch() {
        LocalDateTime now = LocalDateTime.now();
        // 优先查询状态为进行中的批次
        ScoreBatch batch = lambdaQuery()
                .eq(ScoreBatch::getStatus, 1)
                .orderByDesc(ScoreBatch::getCreateTime)
                .last("LIMIT 1")
                .one();
        if (batch != null) {
            return batch;
        }
        // 如果没有进行中的批次，查询当前时间在有效期内的批次
        return lambdaQuery()
                .le(ScoreBatch::getStartTime, now)
                .ge(ScoreBatch::getEndTime, now)
                .orderByDesc(ScoreBatch::getCreateTime)
                .last("LIMIT 1")
                .one();
    }

    @Override
    public java.util.List<ScoreBatch> getAvailableBatchesForStudent(Long userId) {
        return lambdaQuery()
                .eq(ScoreBatch::getStatus, 1)
                .eq(ScoreBatch::getDeleted, 0)
                .le(ScoreBatch::getStartTime, java.time.LocalDateTime.now())
                .ge(ScoreBatch::getEndTime, java.time.LocalDateTime.now())
                .orderByDesc(ScoreBatch::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long id) {
        baseMapper.deleteById(id);
        // 同时删除该批次关联的所有学生待办
        studentTaskService.lambdaUpdate()
                .eq(com.jingxuan.entity.StudentTask::getBatchId, id)
                .remove();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishRanking(Long batchId) {
        ScoreBatch batch = baseMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("评分批次不存在");
        }
        // 发布前校验：至少有 1 个已审核作品有评分数据
        long scoredCount = workMapper.countRankedWorksByBatch(batchId);
        if (scoredCount == 0) {
            throw new BusinessException("该批次暂无已评分的作品，无法公示排行榜");
        }

        batch.setRankPublished(1);
        baseMapper.updateById(batch);

        // 刷新排行榜缓存
        rankService.refreshRankCache(batchId);

        // 通知该批次中学生查看排行榜
        List<Work> worksInBatch = workMapper.selectList(
                Wrappers.<Work>lambdaQuery()
                        .eq(Work::getBatchId, batchId)
                        .eq(Work::getStatus, 3));
        for (Work work : worksInBatch) {
            notificationService.sendNotification(
                    work.getSubmitterId(),
                    "排行榜已公示",
                    "您的作品《" + work.getTitle() + "》的评分排名已公布，请查看排行榜",
                    "ranking",
                    batchId
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublishRanking(Long batchId) {
        ScoreBatch batch = baseMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("评分批次不存在");
        }
        batch.setRankPublished(0);
        baseMapper.updateById(batch);

        // 清除排行榜缓存，避免公示取消后仍看到旧数据
        rankService.clearRankCache(batchId);
    }

    @Override
    public boolean isRankPublished(Long batchId) {
        ScoreBatch batch = baseMapper.selectById(batchId);
        return batch != null && Integer.valueOf(1).equals(batch.getRankPublished());
    }

    @Override
    public boolean isWithinScoringPeriod(Long batchId) {
        ScoreBatch batch = baseMapper.selectById(batchId);
        if (batch == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(batch.getStartTime()) && !now.isAfter(batch.getEndTime());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNotice(Long batchId, String noticeTitle, String noticeContent) {
        ScoreBatch batch = baseMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("评分批次不存在");
        }
        batch.setNoticeTitle(noticeTitle);
        batch.setNoticeContent(noticeContent);
        baseMapper.updateById(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishTask(Long batchId) {
        ScoreBatch batch = baseMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("评分批次不存在");
        }
        if (batch.getNoticeTitle() == null || batch.getNoticeContent() == null) {
            throw new BusinessException("请先填写待办要求");
        }

        // 解析班级范围
        Set<Long> classIdSet = parseClassScopes(batch.getClassScopes());
        if (classIdSet.isEmpty()) {
            throw new BusinessException("该批次未设置班级范围，无法发布待办");
        }

        // 查找班级范围内所有启用状态的学生（按 class_id 匹配）
        List<SysUser> students = sysUserMapper.selectList(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getRoleId, 1) // 学生角色
                        .eq(SysUser::getStatus, 1)
                        .eq(SysUser::getDeleted, 0)
                        .in(SysUser::getClassId, classIdSet));
        if (students.isEmpty()) {
            throw new BusinessException("班级范围内暂无学生用户");
        }

        List<Long> userIds = students.stream()
                .map(SysUser::getId)
                .collect(Collectors.toList());

        // 判断是否为重新发布（已存在该批次的待办）
        boolean isRePublish = studentTaskService.lambdaQuery()
                .eq(com.jingxuan.entity.StudentTask::getBatchId, batchId)
                .eq(com.jingxuan.entity.StudentTask::getDeleted, 0)
                .count() > 0;

        // 创建待办任务（如果已存在则更新内容并重置状态）
        studentTaskService.batchCreateTasks(userIds, batchId, batch.getNoticeTitle(), batch.getNoticeContent());

        // 重新发布时额外发送通知提醒学生
        if (isRePublish) {
            notificationService.sendBatchNotification(
                    userIds,
                    "待办要求已更新",
                    "评分批次《" + batch.getBatchName() + "》的待办要求已更新，请注意查看",
                    "TASK_UPDATE",
                    batchId
            );
        }

        log.info("批次待办已发布: batchId={}, title={}, 接收学生数={}", batchId, batch.getNoticeTitle(), userIds.size());
    }

    /**
     * 解析批次的班级范围，返回 class_id 的集合
     * classScopes 格式示例：'["2022_soft_1","2022_soft_2"]' 或 '全校可参与'
     */
    private Set<Long> parseClassScopes(String classScopes) {
        if (classScopes == null || classScopes.isBlank()) {
            return Set.of();
        }
        String trimmed = classScopes.trim();
        // 全校可参与
        if ("全校可参与".equals(trimmed) || "全校".equals(trimmed) || "all".equalsIgnoreCase(trimmed)) {
            // 查询所有班级
            List<SysDict> allClasses = sysDictMapper.selectList(
                    Wrappers.<SysDict>lambdaQuery()
                            .eq(SysDict::getDictType, "class")
                            .eq(SysDict::getDeleted, 0));
            return allClasses.stream()
                    .map(SysDict::getId)
                    .collect(Collectors.toSet());
        }
        // 按班级 dict_value 匹配：查询 sys_dict 获取对应的 id（JSON 数组格式如 ["1","2","3"]）
        Set<String> scopeValues = ClassScopeUtil.parseToStringSet(classScopes);
        if (scopeValues.isEmpty()) {
            return Set.of();
        }
        List<SysDict> matchedDicts = sysDictMapper.selectList(
                Wrappers.<SysDict>lambdaQuery()
                        .eq(SysDict::getDictType, "class")
                        .in(SysDict::getDictValue, scopeValues)
                        .eq(SysDict::getDeleted, 0));
        return matchedDicts.stream()
                .map(SysDict::getId)
                .collect(Collectors.toSet());
    }
}

package com.jingxuan.modules.task.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.StudentTask;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.StudentTaskMapper;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.modules.task.service.StudentTaskService;
import com.jingxuan.util.ClassScopeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentTaskServiceImpl extends ServiceImpl<StudentTaskMapper, StudentTask> implements StudentTaskService {

    private final ScoreBatchMapper scoreBatchMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDictMapper sysDictMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreateTasks(List<Long> userIds, Long batchId, String title, String content) {
        for (Long userId : userIds) {
            // 避免重复创建
            StudentTask existing = getByUserAndBatch(userId, batchId);
            if (existing != null) {
                // 已存在则更新内容和重置状态（重新发布）
                existing.setTitle(title);
                existing.setContent(content);
                existing.setStatus(0);
                existing.setWorkId(null);
                baseMapper.updateById(existing);
                continue;
            }
            StudentTask task = new StudentTask();
            task.setUserId(userId);
            task.setBatchId(batchId);
            task.setTitle(title);
            task.setContent(content);
            task.setStatus(0);
            baseMapper.insert(task);
        }
        log.info("批量创建待办完成: batchId={}, 学生数={}", batchId, userIds.size());
    }

    @Override
    public List<StudentTask> getStudentTasks(Long userId) {
        // 自动补齐：查找该学生班级范围内活跃批次中缺失的待办
        autoCreateMissingTasks(userId);

        List<StudentTask> tasks = lambdaQuery()
                .eq(StudentTask::getUserId, userId)
                .eq(StudentTask::getDeleted, 0)
                .orderByDesc(StudentTask::getCreateTime)
                .list();

        // 填充批次名称和结束时间
        if (!tasks.isEmpty()) {
            List<Long> batchIds = tasks.stream()
                    .map(StudentTask::getBatchId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, ScoreBatch> batchMap = scoreBatchMapper.selectBatchIds(batchIds).stream()
                    .collect(Collectors.toMap(ScoreBatch::getId, b -> b, (a, b) -> a));
            for (StudentTask task : tasks) {
                ScoreBatch batch = batchMap.get(task.getBatchId());
                if (batch != null) {
                    task.setBatchName(batch.getBatchName());
                    task.setEndTime(batch.getEndTime() != null
                            ? batch.getEndTime().toString().replace("T", " ")
                            : null);
                }
            }
        }

        return tasks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, Long workId) {
        StudentTask task = baseMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("待办不存在");
        }
        task.setWorkId(workId);
        task.setStatus(1);
        baseMapper.updateById(task);
        log.info("待办已完成: taskId={}, workId={}", taskId, workId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(Long workId) {
        // 根据 workId 查找待办并重置为已驳回
        StudentTask task = lambdaQuery()
                .eq(StudentTask::getWorkId, workId)
                .eq(StudentTask::getDeleted, 0)
                .one();
        if (task != null) {
            task.setStatus(2);
            baseMapper.updateById(task);
            log.info("待办已驳回: taskId={}, workId={}", task.getId(), workId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetTask(Long workId) {
        StudentTask task = lambdaQuery()
                .eq(StudentTask::getWorkId, workId)
                .eq(StudentTask::getDeleted, 0)
                .one();
        if (task != null) {
            task.setStatus(0);
            task.setWorkId(null);
            baseMapper.updateById(task);
            log.info("待办已重置: taskId={}, workId={}", task.getId(), workId);
        }
    }

    @Override
    public StudentTask getByUserAndBatch(Long userId, Long batchId) {
        return lambdaQuery()
                .eq(StudentTask::getUserId, userId)
                .eq(StudentTask::getBatchId, batchId)
                .eq(StudentTask::getDeleted, 0)
                .one();
    }

    /**
     * 自动补齐：查找该学生班级范围内活跃批次中缺失的待办记录。
     * 解决「发布批次后注册的学生看不到待办」的问题。
     */
    @Transactional(rollbackFor = Exception.class)
    protected void autoCreateMissingTasks(Long userId) {
        // 查找学生所属班级
        SysUser student = sysUserMapper.selectById(userId);
        if (student == null || student.getClassId() == null) {
            return;
        }

        // 查找当前活跃的批次（进行中且在有效期）
        LocalDateTime now = LocalDateTime.now();
        List<ScoreBatch> activeBatches = scoreBatchMapper.selectList(
                Wrappers.<ScoreBatch>lambdaQuery()
                        .eq(ScoreBatch::getStatus, 1)
                        .eq(ScoreBatch::getDeleted, 0)
                        .le(ScoreBatch::getStartTime, now)
                        .ge(ScoreBatch::getEndTime, now));

        if (activeBatches.isEmpty()) {
            return;
        }

        // 获取该学生已有的待办 batchId 集合
        Set<Long> existingBatchIds = lambdaQuery()
                .eq(StudentTask::getUserId, userId)
                .eq(StudentTask::getDeleted, 0)
                .list()
                .stream()
                .map(StudentTask::getBatchId)
                .collect(Collectors.toSet());

        // 查询学生班级的 dict_value，用于匹配 classScopes
        SysDict studentClass = sysDictMapper.selectById(student.getClassId());
        String studentClassValue = studentClass != null ? studentClass.getDictValue() : null;

        for (ScoreBatch batch : activeBatches) {
            if (existingBatchIds.contains(batch.getId())) {
                continue;
            }
            if (isStudentInBatchScope(batch.getClassScopes(), studentClassValue)) {
                StudentTask task = new StudentTask();
                task.setUserId(userId);
                task.setBatchId(batch.getId());
                task.setTitle(batch.getNoticeTitle());
                task.setContent(batch.getNoticeContent());
                task.setStatus(0);
                baseMapper.insert(task);
                log.info("自动补齐待办: userId={}, batchId={}, batchName={}",
                        userId, batch.getId(), batch.getBatchName());
            }
        }
    }

    /**
     * 判断学生班级是否在批次的班级范围内
     */
    private boolean isStudentInBatchScope(String classScopes, String studentClassValue) {
        if (classScopes == null || classScopes.isBlank()) {
            return false;
        }
        String trimmed = classScopes.trim();
        // 全校可参与
        if ("全校可参与".equals(trimmed) || "全校".equals(trimmed) || "all".equalsIgnoreCase(trimmed)) {
            return true;
        }
        // 按 dict_value 匹配
        if (studentClassValue == null) {
            return false;
        }
        return ClassScopeUtil.parseToStringSet(classScopes).contains(studentClassValue);
    }
}

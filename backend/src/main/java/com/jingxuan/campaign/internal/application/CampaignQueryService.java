package com.jingxuan.campaign.internal.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysUser;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.modules.task.service.StudentTaskService;
import com.jingxuan.campaign.api.V1Batch;
import com.jingxuan.campaign.api.V1Task;
import com.jingxuan.util.ClassScopeUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 批次与待办读取用例，兼容旧 classScopes 存储。 */
@Service
public class CampaignQueryService {
    private final ScoreBatchMapper batchMapper;
    private final SysUserMapper userMapper;
    private final SysDictMapper dictMapper;
    private final StudentTaskService studentTaskService;

    public CampaignQueryService(ScoreBatchMapper batchMapper, SysUserMapper userMapper,
                                SysDictMapper dictMapper, StudentTaskService studentTaskService) {
        this.batchMapper = batchMapper;
        this.userMapper = userMapper;
        this.dictMapper = dictMapper;
        this.studentTaskService = studentTaskService;
    }

    public List<V1Batch> availableBatches(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getClassId() == null) return List.of();
        SysDict clazz = dictMapper.selectById(user.getClassId());
        String classId = user.getClassId().toString();
        String classValue = clazz == null ? null : clazz.getDictValue();
        LocalDateTime now = LocalDateTime.now();
        return batchMapper.selectList(Wrappers.<ScoreBatch>lambdaQuery()
                        .eq(ScoreBatch::getStatus, 1)
                        .le(ScoreBatch::getStartTime, now)
                        .ge(ScoreBatch::getEndTime, now))
                .stream()
                .filter(batch -> {
                    var scopes = ClassScopeUtil.parseToStringSet(batch.getClassScopes());
                    return scopes.contains(classId) || (classValue != null && scopes.contains(classValue));
                })
                .map(V1Batch::from).toList();
    }

    public List<V1Task> myTasks(Long userId) {
        return studentTaskService.getStudentTasks(userId).stream().map(V1Task::from).toList();
    }
}

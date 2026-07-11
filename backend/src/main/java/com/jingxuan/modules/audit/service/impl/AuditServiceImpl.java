package com.jingxuan.modules.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkAudit;
import com.jingxuan.enums.AuditStatusEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkAuditMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkPublishMapper;
import com.jingxuan.modules.audit.dto.AuditHistoryVO;
import com.jingxuan.modules.audit.dto.AuditRequest;
import com.jingxuan.modules.audit.service.AuditService;
import com.jingxuan.modules.log.service.LogService;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.publish.service.PublishService;
import com.jingxuan.modules.task.service.StudentTaskService;
import com.jingxuan.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审核 服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final WorkMapper workMapper;
    private final WorkAuditMapper workAuditMapper;
    private final WorkPublishMapper workPublishMapper;
    private final PublishService publishService;
    private final NotificationService notificationService;
    private final SysUserMapper sysUserMapper;
    private final LogService logService;
    private final StudentTaskService studentTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(AuditRequest request) {
        doAudit(request, AuditStatusEnum.APPROVED.getValue(), 1, null,
                "作品审核通过", "审核通过", true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(AuditRequest request) {
        doAudit(request, AuditStatusEnum.REJECTED.getValue(), 0, request.getReason(),
                "作品审核未通过", "审核驳回", false);
    }

    /**
     * 统一执行审核操作（通过/驳回）
     */
    private void doAudit(AuditRequest request, int targetStatus, int auditResult, String reason,
                         String notifyTitle, String logAction, boolean initPublish) {
        Work work = workMapper.selectById(request.getWorkId());
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        if (work.getStatus() != AuditStatusEnum.SUBMITTED.getValue()) {
            throw new BusinessException("当前状态不可审核");
        }

        work.setStatus(targetStatus);
        workMapper.updateById(work);

        WorkAudit audit = new WorkAudit();
        audit.setWorkId(request.getWorkId());
        audit.setAuditorId(SecurityUtils.requireCurrentUserId());
        audit.setResult(auditResult);
        audit.setReason(reason);
        audit.setAuditTime(LocalDateTime.now());
        workAuditMapper.insert(audit);

        if (initPublish) {
            publishService.initPublish(request.getWorkId());
        }

        String notifyMessage = "您的作品《" + work.getTitle() + "》"
                + (auditResult == 1 ? "已通过审核" : "未通过审核" + (reason != null ? "，原因：" + reason : ""));
        notificationService.sendNotification(
                work.getSubmitterId(),
                notifyTitle,
                notifyMessage,
                "audit",
                work.getId()
        );

        // 审核驳回时重置关联待办状态为"已驳回"
        if (auditResult == 0 && work.getBatchId() != null) {
            com.jingxuan.entity.StudentTask task = studentTaskService.getByUserAndBatch(work.getSubmitterId(), work.getBatchId());
            if (task != null && task.getStatus() != 0) {
                studentTaskService.rejectTask(work.getId());
            }
        }

        logService.recordAction(logAction, "作品", work.getId());
    }

    @Override
    public PageResult<AuditHistoryVO> queryHistory(Long workId, int pageNum, int pageSize) {
        // 分页查询审核记录
        Page<WorkAudit> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WorkAudit> wrapper = Wrappers.<WorkAudit>lambdaQuery()
                .eq(WorkAudit::getWorkId, workId)
                .orderByDesc(WorkAudit::getCreateTime);
        Page<WorkAudit> result = workAuditMapper.selectPage(page, wrapper);

        // 获取作品标题
        Work work = workMapper.selectById(workId);
        String workTitle = work != null ? work.getTitle() : "";

        // 收集审核人 ID 并批量查询姓名
        List<Long> auditorIds = result.getRecords().stream()
                .map(WorkAudit::getAuditorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> auditorNameMap = auditorIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            SysUser user = sysUserMapper.selectById(id);
                            return user != null ? user.getRealName() : "";
                        }
                ));

        // 转换为 VO
        List<AuditHistoryVO> voList = result.getRecords().stream().map(audit -> {
            AuditHistoryVO vo = new AuditHistoryVO();
            vo.setId(audit.getId());
            vo.setWorkId(audit.getWorkId());
            vo.setWorkTitle(workTitle);
            vo.setAuditorName(auditorNameMap.getOrDefault(audit.getAuditorId(), ""));
            vo.setResult(audit.getResult());
            vo.setResultLabel(audit.getResult() == 1 ? "通过" : "驳回");
            vo.setReason(audit.getReason());
            vo.setAuditTime(audit.getAuditTime());
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), pageNum, pageSize);
    }
}

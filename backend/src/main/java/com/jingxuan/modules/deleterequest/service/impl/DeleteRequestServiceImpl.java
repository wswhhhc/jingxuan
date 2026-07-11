package com.jingxuan.modules.deleterequest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.entity.DeleteRequest;
import com.jingxuan.common.PageResult;
import com.jingxuan.common.PageUtil;
import com.jingxuan.entity.DeleteRequest;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.DeleteRequestMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.deleterequest.service.DeleteRequestService;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.work.service.WorkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteRequestServiceImpl extends ServiceImpl<DeleteRequestMapper, DeleteRequest> implements DeleteRequestService {

    private final WorkMapper workMapper;
    private final WorkService workService;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitRequest(Long workId, Long studentId, String reason) {
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        if (work.getStatus() != 3) {
            throw new BusinessException("仅已审核通过的作品可以申请删除");
        }
        if (!work.getSubmitterId().equals(studentId)) {
            throw new BusinessException("只能申请删除自己的作品");
        }

        // 检查是否已有待处理的申请
        long pending = baseMapper.selectCount(Wrappers.<DeleteRequest>lambdaQuery()
                .eq(DeleteRequest::getWorkId, workId)
                .eq(DeleteRequest::getStatus, 0)
                .eq(DeleteRequest::getDeleted, 0));
        if (pending > 0) {
            throw new BusinessException("该作品已有待处理的删除申请");
        }

        DeleteRequest request = new DeleteRequest();
        request.setWorkId(workId);
        request.setStudentId(studentId);
        request.setReason(reason);
        request.setStatus(0);
        baseMapper.insert(request);
        log.info("删除申请已提交: workId={}, studentId={}, reason={}", workId, studentId, reason);
        return request.getId();
    }

    @Override
    public PageResult<DeleteRequest> queryRequests(int pageNum, int pageSize, Integer status) {
        PageResult<DeleteRequest> pageResult = PageUtil.query(pageNum, pageSize, baseMapper,
                w -> {
                    if (status != null) {
                        w.eq(DeleteRequest::getStatus, status);
                    }
                    w.orderByDesc(DeleteRequest::getCreateTime);
                });

        // 补作品名称和申请人姓名
        List<DeleteRequest> records = pageResult.getRecords();
        if (!records.isEmpty()) {
            List<Long> workIds = records.stream().map(DeleteRequest::getWorkId).collect(Collectors.toList());
            List<Long> studentIds = records.stream().map(DeleteRequest::getStudentId).collect(Collectors.toList());

            Map<Long, String> workTitleMap = workMapper.selectBatchIds(workIds).stream()
                    .collect(Collectors.toMap(Work::getId, Work::getTitle, (a, b) -> a));
            Map<Long, String> studentNameMap = sysUserMapper.selectBatchIds(studentIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));

            for (DeleteRequest req : records) {
                req.setWorkTitle(workTitleMap.getOrDefault(req.getWorkId(), "未知作品"));
                req.setStudentName(studentNameMap.getOrDefault(req.getStudentId(), "未知用户"));
            }
        }

        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long requestId, Long adminId) {
        DeleteRequest request = baseMapper.selectById(requestId);
        if (request == null || request.getDeleted() == 1) {
            throw new BusinessException("删除申请不存在");
        }
        if (request.getStatus() != 0) {
            throw new BusinessException("该申请已处理");
        }

        request.setStatus(1);
        baseMapper.updateById(request);

        // 执行作品删除
        workService.adminDeleteWork(request.getWorkId());

        // 通知申请人
        notificationService.sendNotification(
                request.getStudentId(),
                "删除申请已通过",
                "您的作品删除申请已通过，作品已被删除",
                "delete_approved",
                request.getWorkId()
        );

        log.info("删除申请已同意: requestId={}, workId={}, adminId={}", requestId, request.getWorkId(), adminId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long requestId, Long adminId, String reply) {
        DeleteRequest request = baseMapper.selectById(requestId);
        if (request == null || request.getDeleted() == 1) {
            throw new BusinessException("删除申请不存在");
        }
        if (request.getStatus() != 0) {
            throw new BusinessException("该申请已处理");
        }

        request.setStatus(2);
        request.setAdminReply(reply);
        baseMapper.updateById(request);

        // 通知申请人
        String replyMsg = reply != null ? "，原因：" + reply : "";
        notificationService.sendNotification(
                request.getStudentId(),
                "删除申请未通过",
                "您的作品删除申请未通过" + replyMsg,
                "delete_rejected",
                request.getWorkId()
        );

        log.info("删除申请已拒绝: requestId={}, workId={}, adminId={}", requestId, request.getWorkId(), adminId);
    }
}

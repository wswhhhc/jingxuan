package com.jingxuan.modules.adapter;

import com.jingxuan.common.Result;
import com.jingxuan.entity.SysUser;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/notify")
@Tag(name = "管理端消息通知")
public class AdminNotifyController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "发送通知至指定班级学生")
    public Result<Void> sendBatchNotice(@RequestBody SendNoticeRequest req) {
        // 1. 解析接收用户范围
        List<Long> userIds;
        if (Boolean.TRUE.equals(req.getAllSchool())) {
            userIds = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRoleId, 1)
                    .eq(SysUser::getDeleted, 0)
                    .eq(SysUser::getStatus, 1)
            ).stream().map(SysUser::getId).collect(Collectors.toList());
        } else if (req.getClassIds() != null && !req.getClassIds().isEmpty()) {
            userIds = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                    .in(SysUser::getClassId, req.getClassIds())
                    .eq(SysUser::getRoleId, 1)
                    .eq(SysUser::getDeleted, 0)
                    .eq(SysUser::getStatus, 1)
            ).stream().map(SysUser::getId).collect(Collectors.toList());
        } else {
            return Result.fail("请指定通知范围");
        }

        if (userIds.isEmpty()) {
            return Result.fail("所选范围内暂无学生用户");
        }

        // 2. 发送通知
        notificationService.sendBatchNotification(
            userIds,
            req.getTitle(),
            req.getContent(),
            "SYSTEM",
            req.getBatchId()
        );

        return Result.ok();
    }

    public static class SendNoticeRequest {
        private String title;
        private String content;
        private Long batchId;
        private Boolean allSchool;
        private List<Long> classIds;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }
        public Boolean getAllSchool() { return allSchool; }
        public void setAllSchool(Boolean allSchool) { this.allSchool = allSchool; }
        public List<Long> getClassIds() { return classIds; }
        public void setClassIds(List<Long> classIds) { this.classIds = classIds; }
    }
}

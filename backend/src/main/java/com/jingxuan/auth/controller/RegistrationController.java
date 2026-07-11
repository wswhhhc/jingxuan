package com.jingxuan.auth.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.common.Result;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysNotification;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysNotificationMapper;
import com.jingxuan.auth.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "注册")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;
    private final ScoreBatchMapper scoreBatchMapper;
    private final SysDictMapper sysDictMapper;
    private final SysNotificationMapper sysNotificationMapper;

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestBody Map<String, Object> body) {
        registrationService.sendVerificationCode(body);
        return Result.ok();
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = registrationService.register(body);

        // 新学生注册后自动获取批次通知
        Object roleIdObj = body.get("roleId");
        Integer roleId = roleIdObj instanceof Number ? ((Number) roleIdObj).intValue() : null;
        if (roleId != null && roleId == 1) {
            Long classId = parseOptionalLong(body.get("classId"));
            if (classId != null) {
                Long studentId = (Long) result.get("id");
                generateBatchNoticesForNewStudent(studentId, classId);
            }
        }

        return Result.ok("注册成功", result);
    }

    private void generateBatchNoticesForNewStudent(Long studentId, Long classId) {
        SysDict classDict = sysDictMapper.selectById(classId);
        if (classDict == null) return;

        String classValue = classDict.getDictValue();
        if (classValue == null || classValue.isBlank()) return;

        LocalDateTime now = LocalDateTime.now();

        List<ScoreBatch> activeBatches = scoreBatchMapper.selectList(
                Wrappers.<ScoreBatch>lambdaQuery()
                        .eq(ScoreBatch::getStatus, 1)
                        .eq(ScoreBatch::getDeleted, 0)
                        .le(ScoreBatch::getStartTime, now)
                        .ge(ScoreBatch::getEndTime, now)
                        .isNotNull(ScoreBatch::getNoticeTitle)
                        .isNotNull(ScoreBatch::getNoticeContent));

        for (ScoreBatch batch : activeBatches) {
            String scopes = batch.getClassScopes();
            if (scopes == null || scopes.isBlank()) continue;

            boolean inScope = isClassInScope(classValue, scopes);
            if (inScope) {
                // 检查是否已有该通知（防止重复）
                Long count = sysNotificationMapper.selectCount(
                        Wrappers.<SysNotification>lambdaQuery()
                                .eq(SysNotification::getUserId, studentId)
                                .eq(SysNotification::getType, "BATCH_NOTICE")
                                .eq(SysNotification::getRefId, batch.getId()));
                if (count == 0) {
                    try {
                        SysNotification notification = new SysNotification();
                        notification.setUserId(studentId);
                        notification.setTitle(batch.getNoticeTitle());
                        notification.setContent(batch.getNoticeContent());
                        notification.setType("BATCH_NOTICE");
                        notification.setRefId(batch.getId());
                        notification.setIsRead(0);
                        sysNotificationMapper.insert(notification);
                    } catch (Exception e) {
                        log.warn("为新学生生成批次通知失败: studentId={}, batchId={}", studentId, batch.getId(), e);
                    }
                }
            }
        }
    }

    private boolean isClassInScope(String classValue, String scopes) {
        String trimmed = scopes.trim();
        if ("全校可参与".equals(trimmed) || "全校".equals(trimmed) || "all".equalsIgnoreCase(trimmed)) {
            return true;
        }
        try {
            return JSONUtil.parseArray(trimmed).toList(String.class)
                    .stream().anyMatch(s -> s.equals(classValue));
        } catch (Exception e) {
            // 兼容旧数据中的逗号分隔格式
            return Arrays.stream(trimmed.replace("[", "").replace("]", "").replace("\"", "").split("[,，]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .anyMatch(s -> s.equals(classValue));
        }
    }

    private Long parseOptionalLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.valueOf(value.toString()); } catch (NumberFormatException e) { return null; }
    }
}

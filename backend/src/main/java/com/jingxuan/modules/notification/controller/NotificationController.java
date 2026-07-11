package com.jingxuan.modules.notification.controller;

import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.SysNotification;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息通知接口
 *
 * 统一处理所有角色的通知查询/已读/未读数接口，
 * 消除 AdminApiController / TeacherApiController / StudentApiController 中的重复代码。
 */
@Tag(name = "消息通知")
@RestController
@RequestMapping({"/admin/notify", "/teacher/notify", "/student/notify"})
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "查询用户通知列表")
    @GetMapping("/list")
    public Result<PageResult<SysNotification>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean unreadOnly) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.ok(notificationService.queryUserNotifications(userId, page, size, unreadOnly));
    }

    @Operation(summary = "标记通知已读")
    @PostMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.ok();
    }

    @Operation(summary = "获取未读通知数")
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        Long userId = SecurityUtils.requireCurrentUserId();
        long count = notificationService.countUnread(userId);
        Map<String, Long> map = new HashMap<>();
        map.put("count", count);
        return Result.ok(map);
    }

    @Operation(summary = "删除已读通知")
    @DeleteMapping("/read")
    public Result<Void> deleteRead() {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.deleteRead(userId);
        return Result.ok();
    }
}

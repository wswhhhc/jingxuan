package com.jingxuan.communication.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Page;
import com.jingxuan.communication.api.V1Notification;
import com.jingxuan.communication.internal.application.NotificationQueryService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@V1Api
@RestController
@RequestMapping("/api/v1/me/notifications")
@Tag(name = "个人通知")
public class V1NotificationController {

    private final NotificationQueryService notificationQueryService;

    public V1NotificationController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "查询当前用户通知列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "通知列表"))
    public V1Page<V1Notification> queryNotifications(
            @RequestParam(defaultValue = "1") @jakarta.validation.constraints.Min(1) int page,
            @RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int pageSize,
            @RequestParam(required = false) Boolean unreadOnly) {
        return notificationQueryService.queryNotifications(SecurityUtils.requireCurrentUserId(), page, pageSize, unreadOnly);
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取当前用户未读通知数")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "未读数"))
    public long getUnreadCount() {
        return notificationQueryService.countUnread(SecurityUtils.requireCurrentUserId());
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "标记单条通知为已读")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "标记成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public void markAsRead(@PathVariable String id) {
        notificationQueryService.markAsRead(id, SecurityUtils.requireCurrentUserId());
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "全部标记为已读")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "标记成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public void markAllAsRead() {
        notificationQueryService.markAllAsRead(SecurityUtils.requireCurrentUserId());
    }

    @DeleteMapping("/read")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除当前用户所有已读通知")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public void deleteRead() {
        notificationQueryService.deleteRead(SecurityUtils.requireCurrentUserId());
    }
}

package com.jingxuan.modules.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SysNotification;

public interface NotificationService extends IService<SysNotification> {

    /**
     * 发送通知
     */
    void sendNotification(Long userId, String title, String content, String type, Long refId);

    /**
     * 发送批量通知
     */
    void sendBatchNotification(java.util.List<Long> userIds, String title, String content, String type, Long refId);

    /**
     * 查询用户通知列表
     * @param unreadOnly true 仅返回未读，false/null 返回全部
     */
    PageResult<SysNotification> queryUserNotifications(Long userId, int pageNum, int pageSize, Boolean unreadOnly);

    /**
     * 标记已读
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 全部标记已读
     */
    void markAllAsRead(Long userId);

    /**
     * 获取未读通知数
     */
    long countUnread(Long userId);

    /**
     * 删除当前用户的所有已读通知
     */
    void deleteRead(Long userId);
}

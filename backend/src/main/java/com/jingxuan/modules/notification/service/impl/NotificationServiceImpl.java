package com.jingxuan.modules.notification.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.common.PageResult;
import com.jingxuan.common.PageUtil;
import com.jingxuan.entity.SysNotification;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysNotificationMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统通知 服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl extends ServiceImpl<SysNotificationMapper, SysNotification> implements NotificationService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(Long userId, String title, String content, String type, Long refId) {
        SysNotification notification = new SysNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setRefId(refId);
        notification.setIsRead(0);
        baseMapper.insert(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendBatchNotification(List<Long> userIds, String title, String content, String type, Long refId) {
        for (Long userId : userIds) {
            SysNotification notification = new SysNotification();
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setType(type);
            notification.setRefId(refId);
            notification.setIsRead(0);
            baseMapper.insert(notification);
        }
    }

    @Override
    public PageResult<SysNotification> queryUserNotifications(Long userId, int pageNum, int pageSize, Boolean unreadOnly) {
        return PageUtil.query(pageNum, pageSize, baseMapper,
                w -> w.eq(SysNotification::getUserId, userId)
                      .eq(unreadOnly != null && unreadOnly, SysNotification::getIsRead, 0)
                      .orderByDesc(SysNotification::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId, Long userId) {
        SysNotification notification = baseMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此通知");
        }
        notification.setIsRead(1);
        notification.setReadTime(LocalDateTime.now());
        baseMapper.updateById(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        baseMapper.update(null,
                Wrappers.<SysNotification>lambdaUpdate()
                        .set(SysNotification::getIsRead, 1)
                        .set(SysNotification::getReadTime, LocalDateTime.now())
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getIsRead, 0));
    }

    @Override
    public long countUnread(Long userId) {
        return baseMapper.selectCount(
                Wrappers.<SysNotification>lambdaQuery()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getIsRead, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRead(Long userId) {
        baseMapper.delete(
                Wrappers.<SysNotification>lambdaQuery()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getIsRead, 1));
    }
}

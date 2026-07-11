package com.jingxuan.modules.notice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.common.PageResult;
import com.jingxuan.common.PageUtil;
import com.jingxuan.entity.SysNotice;
import com.jingxuan.entity.SysUser;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.SysNoticeMapper;
import com.jingxuan.modules.notice.dto.NoticeRequest;
import com.jingxuan.modules.notice.service.NoticeService;
import com.jingxuan.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 公告 服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements NoticeService {

    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNotice(NoticeRequest request, Long publisherId) {
        SysNotice notice = new SysNotice();
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setTopFlag(request.getTopFlag());
        notice.setStatus(request.getStatus());
        notice.setTargetScope(request.getTargetScope());
        notice.setPublisherId(publisherId);
        if (request.getStatus() != null && request.getStatus() == 1) {
            notice.setPublishTime(LocalDateTime.now());
        }
        baseMapper.insert(notice);
        // 已发布状态则发送通知
        if (request.getStatus() != null && request.getStatus() == 1) {
            sendNotificationByScope(request.getTargetScope(), request.getTitle(), request.getContent(), notice.getId());
        }
        return notice.getId();
    }

    /**
     * 根据目标范围发送系统通知
     */
    private void sendNotificationByScope(String targetScope, String title, String content, Long noticeId) {
        try {
            List<Integer> roleIds;
            if ("teacher".equals(targetScope)) {
                roleIds = List.of(2);
            } else if ("student".equals(targetScope)) {
                roleIds = List.of(1);
            } else {
                // all 或未指定则发送给全体（学生 + 教师）
                roleIds = List.of(1, 2);
            }

            List<Long> userIds = sysUserMapper.selectList(
                    Wrappers.<SysUser>lambdaQuery()
                            .in(SysUser::getRoleId, roleIds)
                            .eq(SysUser::getDeleted, 0)
                            .eq(SysUser::getStatus, 1)
            ).stream().map(SysUser::getId).collect(Collectors.toList());

            if (!userIds.isEmpty()) {
                notificationService.sendBatchNotification(
                        userIds,
                        "系统公告：" + title,
                        content,
                        "NOTICE",
                        noticeId
                );
                log.info("公告通知已发送给 {} 人(scope={}): noticeId={}", userIds.size(), targetScope, noticeId);
            }
        } catch (Exception e) {
            log.warn("发送公告通知失败，不影响公告发布: noticeId={}", noticeId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotice(Long id, NoticeRequest request) {
        SysNotice notice = new SysNotice();
        notice.setId(id);
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setTopFlag(request.getTopFlag());
        notice.setStatus(request.getStatus());
        if (request.getStatus() != null && request.getStatus() == 1) {
            notice.setPublishTime(LocalDateTime.now());
        }
        notice.setTargetScope(request.getTargetScope());
        baseMapper.updateById(notice);
        // 更新为已发布时发送通知
        if (request.getStatus() != null && request.getStatus() == 1) {
            sendNotificationByScope(request.getTargetScope(), request.getTitle(), request.getContent(), id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishNotice(Long id) {
        SysNotice notice = baseMapper.selectById(id);
        if (notice == null) {
            return;
        }
        notice.setStatus(1);
        notice.setPublishTime(LocalDateTime.now());
        baseMapper.updateById(notice);
        sendNotificationByScope(notice.getTargetScope(), notice.getTitle(), notice.getContent(), id);
    }

    @Override
    public PageResult<SysNotice> queryNoticeList(int pageNum, int pageSize, Integer status) {
        PageResult<SysNotice> result = PageUtil.query(pageNum, pageSize, baseMapper,
                w -> w.eq(status != null, SysNotice::getStatus, status)
                      .orderByDesc(SysNotice::getTopFlag)
                      .orderByDesc(SysNotice::getCreateTime));
        enrichPublisherNames(result.getRecords());
        return result;
    }

    @Override
    public PageResult<SysNotice> getPublishedNotices(int pageNum, int pageSize) {
        PageResult<SysNotice> result = PageUtil.query(pageNum, pageSize, baseMapper,
                w -> w.eq(SysNotice::getStatus, 1)
                      .orderByDesc(SysNotice::getTopFlag)
                      .orderByDesc(SysNotice::getPublishTime));
        enrichPublisherNames(result.getRecords());
        return result;
    }

    private void enrichPublisherNames(List<SysNotice> notices) {
        List<Long> publisherIds = notices.stream()
                .map(SysNotice::getPublisherId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (publisherIds.isEmpty()) {
            return;
        }

        Map<Long, String> userNameMap = sysUserMapper.selectBatchIds(publisherIds).stream()
                .collect(Collectors.toMap(
                        SysUser::getId,
                        user -> {
                            if (user.getRealName() != null && !user.getRealName().isBlank()) {
                                return user.getRealName();
                            }
                            return user.getUsername();
                        },
                        (left, right) -> left
                ));

        notices.forEach(notice -> notice.setPublisherName(userNameMap.get(notice.getPublisherId())));
    }
}

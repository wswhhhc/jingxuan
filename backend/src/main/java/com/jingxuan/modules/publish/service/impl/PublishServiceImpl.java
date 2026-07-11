package com.jingxuan.modules.publish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.constant.CommonConstants;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkPublish;
import com.jingxuan.enums.AuditStatusEnum;
import com.jingxuan.enums.PublishStatusEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkPublishMapper;
import com.jingxuan.modules.log.service.LogService;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.publish.dto.FeaturedRequest;
import com.jingxuan.modules.publish.service.PublishService;
import com.jingxuan.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 发布 服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PublishServiceImpl implements PublishService {

    private final WorkMapper workMapper;
    private final WorkPublishMapper workPublishMapper;
    private final NotificationService notificationService;
    private final LogService logService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initPublish(Long workId) {
        WorkPublish existing = workPublishMapper.selectByWorkId(workId);
        if (existing != null) {
            return;
        }
        WorkPublish publish = new WorkPublish();
        publish.setWorkId(workId);
        publish.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getValue());
        workPublishMapper.insert(publish);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishWork(Long workId) {
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        if (work.getStatus() != AuditStatusEnum.APPROVED.getValue()) {
            throw new BusinessException("作品尚未通过审核，无法发布");
        }

        WorkPublish publish = workPublishMapper.selectByWorkId(workId);
        if (publish == null) {
            throw new BusinessException("发布记录不存在，请先初始化");
        }

        publish.setPublishStatus(PublishStatusEnum.PUBLISHED.getValue());
        publish.setPublishTime(LocalDateTime.now());
        publish.setPublisherId(SecurityUtils.requireCurrentUserId());
        workPublishMapper.updateById(publish);

        notificationService.sendNotification(
                work.getSubmitterId(),
                "作品已发布",
                "您的作品《" + work.getTitle() + "》已成功发布",
                "publish",
                workId
        );

        logService.recordAction("发布作品", "作品", workId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineWork(Long workId) {
        WorkPublish publish = workPublishMapper.selectByWorkId(workId);
        if (publish == null) {
            throw new BusinessException("发布记录不存在");
        }

        publish.setPublishStatus(PublishStatusEnum.OFFLINE.getValue());
        publish.setOfflineTime(LocalDateTime.now());
        workPublishMapper.updateById(publish);

        Work work = workMapper.selectById(workId);
        if (work != null) {
            notificationService.sendNotification(
                    work.getSubmitterId(),
                    "作品已下线",
                    "您的作品《" + work.getTitle() + "》已被下线",
                    "publish",
                    workId
            );
        }

        logService.recordAction("作品下线", "作品", workId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setFeatured(FeaturedRequest request) {
        // 设为精选时检查上限
        if (request.getFeatured() == CommonConstants.FEATURED_YES) {
            long featuredCount = workPublishMapper.selectCount(
                    Wrappers.<WorkPublish>lambdaQuery()
                            .eq(WorkPublish::getFeatured, CommonConstants.FEATURED_YES)
            );
            if (featuredCount >= CommonConstants.MAX_FEATURED_COUNT) {
                throw new BusinessException("精选作品已达上限（最多" + CommonConstants.MAX_FEATURED_COUNT + "个）");
            }
        }

        WorkPublish publish = workPublishMapper.selectByWorkId(request.getWorkId());
        if (publish == null) {
            throw new BusinessException("发布记录不存在");
        }

        publish.setFeatured(request.getFeatured());
        publish.setPreviewUrl(request.getPreviewUrl());
        workPublishMapper.updateById(publish);
    }

    @Override
    public WorkPublish getByWorkId(Long workId) {
        return workPublishMapper.selectByWorkId(workId);
    }
}

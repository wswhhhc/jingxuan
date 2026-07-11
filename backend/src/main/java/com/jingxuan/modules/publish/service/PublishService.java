package com.jingxuan.modules.publish.service;

import com.jingxuan.entity.WorkPublish;
import com.jingxuan.modules.publish.dto.FeaturedRequest;

public interface PublishService {

    /**
     * 发布作品（审核通过后，将发布状态设为已发布）
     */
    void publishWork(Long workId);

    /**
     * 下线作品
     */
    void offlineWork(Long workId);

    /**
     * 设置精选标记与预览地址
     */
    void setFeatured(FeaturedRequest request);

    /**
     * 获取作品发布信息
     */
    WorkPublish getByWorkId(Long workId);

    /**
     * 初始化发布记录（审核通过时调用）
     */
    void initPublish(Long workId);
}

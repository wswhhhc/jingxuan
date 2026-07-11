package com.jingxuan.modules.notice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SysNotice;
import com.jingxuan.modules.notice.dto.NoticeRequest;

public interface NoticeService extends IService<SysNotice> {

    /**
     * 创建公告
     */
    Long createNotice(NoticeRequest request, Long publisherId);

    /**
     * 更新公告
     */
    void updateNotice(Long id, NoticeRequest request);

    /**
     * 发布公告
     */
    void publishNotice(Long id);

    /**
     * 分页查询公告
     */
    PageResult<SysNotice> queryNoticeList(int pageNum, int pageSize, Integer status);

    /**
     * 获取前台可见的已发布公告列表
     */
    PageResult<SysNotice> getPublishedNotices(int pageNum, int pageSize);
}

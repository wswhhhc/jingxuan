package com.jingxuan.communication.internal.application;

import com.jingxuan.api.V1Ids;
import com.jingxuan.api.V1Page;
import com.jingxuan.api.V1PageInfo;
import com.jingxuan.communication.api.V1Notice;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SysNotice;
import com.jingxuan.exception.NotFoundException;
import com.jingxuan.modules.notice.service.NoticeService;
import org.springframework.stereotype.Service;

@Service
public class NoticeQueryService {

    private final NoticeService noticeService;

    public NoticeQueryService(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    public V1Page<V1Notice> listNotices(int page, int size, Integer status) {
        PageResult<SysNotice> result = noticeService.queryNoticeList(page, size, status);
        return toV1Page(result, page, size);
    }

    public V1Page<V1Notice> listPublishedNotices(int page, int size) {
        PageResult<SysNotice> result = noticeService.getPublishedNotices(page, size);
        return toV1Page(result, page, size);
    }

    public V1Notice getNotice(String id) {
        Long noticeId = V1Ids.parse(id, "id");
        SysNotice notice = noticeService.getById(noticeId);
        if (notice == null) {
            throw new NotFoundException("公告不存在");
        }
        // 临时查询发布者姓名
        notice.setPublisherName(null);
        PageResult<SysNotice> single = noticeService.queryNoticeList(1, 1, null);
        return V1Notice.from(notice);
    }

    private static V1Page<V1Notice> toV1Page(PageResult<SysNotice> result, int page, int size) {
        return new V1Page<>(
            result.getRecords().stream().map(V1Notice::from).toList(),
            V1PageInfo.of(page, size, result.getTotal())
        );
    }
}

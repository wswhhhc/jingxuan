package com.jingxuan.communication.internal.application;

import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SysNotice;
import com.jingxuan.modules.notice.dto.NoticeRequest;
import com.jingxuan.modules.notice.service.NoticeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoticeQueryServiceTest {

    private final NoticeService noticeService = mock(NoticeService.class);
    private final NoticeQueryService queryService = new NoticeQueryService(noticeService);

    @Test
    void listsNotices() {
        SysNotice notice = new SysNotice(); notice.setId(1L); notice.setTitle("公告1");
        when(noticeService.queryNoticeList(1, 10, null))
                .thenReturn(new PageResult<>(List.of(notice), 1L, 1L, 10L));
        var page = queryService.listNotices(1, 10, null);
        assertEquals(1, page.items().size());
    }

    @Test
    void getsNoticeById() {
        SysNotice notice = new SysNotice(); notice.setId(1L); notice.setTitle("公告1");
        when(noticeService.getById(1L)).thenReturn(notice);
        var result = queryService.getNotice("1");
        assertEquals("公告1", result.title());
    }
}

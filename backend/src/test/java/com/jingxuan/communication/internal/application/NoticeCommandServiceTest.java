package com.jingxuan.communication.internal.application;

import com.jingxuan.entity.SysNotice;
import com.jingxuan.modules.notice.service.NoticeService;
import com.jingxuan.communication.api.V1NoticeRequest;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class NoticeCommandServiceTest {

    private final NoticeService noticeService = mock(NoticeService.class);
    private final NoticeCommandService commandService = new NoticeCommandService(noticeService);

    @Test
    void createsNotice() {
        when(noticeService.createNotice(any(), eq(7L))).thenReturn(1L);
        var request = new V1NoticeRequest("标题", "内容", true, "all");
        commandService.createNotice(request, 7L);
        verify(noticeService).createNotice(any(), eq(7L));
    }

    @Test
    void deletesNotice() {
        when(noticeService.getById(1L)).thenReturn(new SysNotice());
        commandService.deleteNotice("1");
        verify(noticeService).removeById(1L);
    }
}

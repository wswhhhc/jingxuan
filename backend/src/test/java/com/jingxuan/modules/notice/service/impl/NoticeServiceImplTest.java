package com.jingxuan.modules.notice.service.impl;

import com.jingxuan.entity.SysNotice;
import com.jingxuan.mapper.SysNoticeMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.modules.notice.dto.NoticeRequest;
import com.jingxuan.modules.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeServiceImpl - 公告服务")
class NoticeServiceImplTest {

    @Mock private SysNoticeMapper sysNoticeMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private NotificationService notificationService;

    @Captor private ArgumentCaptor<SysNotice> noticeCaptor;

    private NoticeServiceImpl noticeService;

    @BeforeEach
    void setUp() {
        noticeService = new NoticeServiceImpl(sysUserMapper, notificationService);
        ReflectionTestUtils.setField(noticeService, "baseMapper", sysNoticeMapper);
    }

    @Nested
    @DisplayName("创建公告")
    class CreateNotice {

        @Test
        @DisplayName("成功创建公告")
        void shouldCreateNotice() {
            NoticeRequest request = new NoticeRequest();
            request.setTitle("系统升级通知");
            request.setContent("系统将于今晚升级");
            request.setStatus(0);
            request.setTopFlag(0);

            when(sysNoticeMapper.insert(any(SysNotice.class))).thenAnswer(inv -> {
                ((SysNotice) inv.getArgument(0)).setId(1L);
                return 1;
            });

            Long id = noticeService.createNotice(request, 300L);

            assertEquals(1L, id);
            verify(sysNoticeMapper).insert(noticeCaptor.capture());
            assertEquals("系统升级通知", noticeCaptor.getValue().getTitle());
            assertEquals(300L, noticeCaptor.getValue().getPublisherId());
        }

        @Test
        @DisplayName("已发布状态创建时自动设置发布时间并发送通知")
        void shouldSetPublishTimeWhenStatusPublished() {
            NoticeRequest request = new NoticeRequest();
            request.setTitle("通知");
            request.setContent("内容");
            request.setStatus(1);

            when(sysNoticeMapper.insert(any(SysNotice.class))).thenAnswer(inv -> {
                ((SysNotice) inv.getArgument(0)).setId(1L);
                return 1;
            });
            when(sysUserMapper.selectList(any())).thenReturn(List.of());

            noticeService.createNotice(request, 300L);

            verify(sysNoticeMapper).insert(noticeCaptor.capture());
            assertNotNull(noticeCaptor.getValue().getPublishTime());
        }
    }

    @Nested
    @DisplayName("更新公告")
    class UpdateNotice {

        @Test
        @DisplayName("成功更新公告")
        void shouldUpdateNotice() {
            NoticeRequest request = new NoticeRequest();
            request.setTitle("新标题");
            request.setContent("新内容");

            noticeService.updateNotice(1L, request);

            verify(sysNoticeMapper).updateById(noticeCaptor.capture());
            assertEquals(1L, noticeCaptor.getValue().getId());
            assertEquals("新标题", noticeCaptor.getValue().getTitle());
        }
    }

    @Nested
    @DisplayName("发布公告")
    class PublishNotice {

        @Test
        @DisplayName("成功发布公告并发送通知")
        void shouldPublishNotice() {
            SysNotice existing = new SysNotice();
            existing.setId(1L);
            existing.setTitle("升级通知");
            existing.setContent("系统升级");

            when(sysNoticeMapper.selectById(1L)).thenReturn(existing);
            when(sysUserMapper.selectList(any())).thenReturn(List.of());

            noticeService.publishNotice(1L);

            verify(sysNoticeMapper).updateById(noticeCaptor.capture());
            assertEquals(1, noticeCaptor.getValue().getStatus().intValue());
            assertNotNull(noticeCaptor.getValue().getPublishTime());
        }
    }
}

package com.jingxuan.communication.internal.application;

import com.jingxuan.api.V1Ids;
import com.jingxuan.communication.api.V1NoticeRequest;
import com.jingxuan.exception.NotFoundException;
import com.jingxuan.modules.notice.dto.NoticeRequest;
import com.jingxuan.modules.notice.service.NoticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeCommandService {

    private final NoticeService noticeService;

    public NoticeCommandService(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Transactional(rollbackFor = Exception.class)
    public String createNotice(V1NoticeRequest request, Long publisherId) {
        NoticeRequest oldReq = toOldRequest(request);
        Long id = noticeService.createNotice(oldReq, publisherId);
        return String.valueOf(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateNotice(String id, V1NoticeRequest request) {
        Long noticeId = V1Ids.parse(id, "id");
        ensureExists(noticeId);
        NoticeRequest oldReq = toOldRequest(request);
        noticeService.updateNotice(noticeId, oldReq);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishNotice(String id) {
        Long noticeId = V1Ids.parse(id, "id");
        ensureExists(noticeId);
        noticeService.publishNotice(noticeId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(String id) {
        Long noticeId = V1Ids.parse(id, "id");
        ensureExists(noticeId);
        noticeService.removeById(noticeId);
    }

    private void ensureExists(Long id) {
        if (noticeService.getById(id) == null) {
            throw new NotFoundException("公告不存在");
        }
    }

    private static NoticeRequest toOldRequest(V1NoticeRequest request) {
        NoticeRequest old = new NoticeRequest();
        old.setTitle(request.title());
        old.setContent(request.content());
        old.setTopFlag(0);
        old.setTargetScope(request.targetScope() != null ? request.targetScope() : "all");
        old.setStatus(request.publishDirectly() ? 1 : 0);
        return old;
    }
}

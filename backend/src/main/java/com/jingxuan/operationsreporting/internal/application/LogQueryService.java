package com.jingxuan.operationsreporting.internal.application;

import com.jingxuan.api.V1Page;
import com.jingxuan.api.V1PageInfo;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SysLog;
import com.jingxuan.modules.log.service.LogService;
import com.jingxuan.operationsreporting.api.V1LogEntry;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class LogQueryService {

    private final LogService logService;

    public LogQueryService(LogService logService) {
        this.logService = logService;
    }

    public V1Page<V1LogEntry> queryLogs(int page, int size, String action, Long userId) {
        PageResult<SysLog> pageResult = logService.queryLogList(page, size, action, userId);

        List<V1LogEntry> items = pageResult.getRecords().stream()
                .map(this::toEntry)
                .toList();

        V1PageInfo pageInfo = V1PageInfo.of(page, size, pageResult.getTotal());
        return new V1Page<>(items, pageInfo);
    }

    private V1LogEntry toEntry(SysLog log) {
        OffsetDateTime createdAt = log.getCreateTime() != null
                ? log.getCreateTime().atOffset(java.time.ZoneOffset.ofHours(8))
                : null;

        return new V1LogEntry(
                log.getId().toString(),
                log.getUserId() != null ? log.getUserId().toString() : null,
                log.getUsername(),
                log.getAction(),
                log.getTarget(),
                log.getTargetId() != null ? log.getTargetId().toString() : null,
                log.getIp(),
                log.getRequestMethod(),
                log.getRequestPath(),
                log.getResult() != null && log.getResult() == 1,
                log.getErrorMsg(),
                log.getDuration(),
                createdAt
        );
    }
}

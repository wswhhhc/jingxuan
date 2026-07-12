package com.jingxuan.operationsreporting.internal.application;

import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SysLog;
import com.jingxuan.modules.log.service.LogService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogQueryServiceTest {

    private final LogService logService = mock(LogService.class);
    private final LogQueryService queryService = new LogQueryService(logService);

    @Test
    void queriesLogs() {
        SysLog log = new SysLog(); log.setId(1L); log.setAction("登录"); log.setResult(1);
        when(logService.queryLogList(1, 20, null, null))
                .thenReturn(new PageResult<>(List.of(log), 1L, 1L, 20L));
        var page = queryService.queryLogs(1, 20, null, null);
        assertEquals(1, page.items().size());
        assertTrue(page.items().get(0).success());
    }

    @Test
    void convertsFailedResult() {
        SysLog log = new SysLog(); log.setId(0L); log.setResult(0);
        when(logService.queryLogList(1, 20, null, null))
                .thenReturn(new PageResult<>(List.of(log), 1L, 1L, 20L));
        assertFalse(queryService.queryLogs(1, 20, null, null).items().get(0).success());
    }
}

package com.jingxuan.modules.log.service.impl;

import com.jingxuan.entity.SysLog;
import com.jingxuan.entity.SysUser;
import com.jingxuan.mapper.SysLogMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogServiceImpl - 日志服务")
class LogServiceImplTest {

    @Mock private SysLogMapper sysLogMapper;
    @Mock private SysUserMapper sysUserMapper;

    @Captor private ArgumentCaptor<SysLog> logCaptor;

    private LogServiceImpl logService;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        logService = new LogServiceImpl(sysUserMapper);
        ReflectionTestUtils.setField(logService, "baseMapper", sysLogMapper);
        securityUtilsMock = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("记录日志")
    class RecordLog {

        @Test
        @DisplayName("成功记录操作日志")
        void shouldRecordLog() {
            logService.recordLog(100L, "张三", "审核通过", "作品", 1L,
                    "192.168.1.1", "POST", "/admin/audit", true, null, 100L);

            verify(sysLogMapper).insert(logCaptor.capture());
            assertEquals(100L, logCaptor.getValue().getUserId());
            assertEquals("张三", logCaptor.getValue().getUsername());
            assertEquals("审核通过", logCaptor.getValue().getAction());
            assertEquals(1, logCaptor.getValue().getResult().intValue());
        }

        @Test
        @DisplayName("记录失败操作日志")
        void shouldRecordFailedLog() {
            logService.recordLog(100L, "张三", "审核失败", "作品", 1L,
                    "", "", "", false, "权限不足", 0L);

            verify(sysLogMapper).insert(logCaptor.capture());
            assertEquals(0, logCaptor.getValue().getResult().intValue());
            assertEquals("权限不足", logCaptor.getValue().getErrorMsg());
        }

        @Test
        @DisplayName("保存失败不影响主业务")
        void shouldNotThrowOnSaveFailure() {
            doThrow(new RuntimeException("DB error")).when(sysLogMapper).insert(any(SysLog.class));

            assertDoesNotThrow(() ->
                logService.recordLog(100L, "张三", "操作", "作品", 1L,
                        "", "", "", true, null, 0L));
        }
    }

    @Nested
    @DisplayName("记录操作（便捷方法）")
    class RecordAction {

        @Test
        @DisplayName("用户已登录时记录操作")
        void shouldRecordActionWhenLoggedIn() {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(100L);
            SysUser user = new SysUser();
            user.setId(100L);
            user.setUsername("张三");
            when(sysUserMapper.selectById(100L)).thenReturn(user);

            logService.recordAction("提交审核", "作品", 1L);

            verify(sysLogMapper).insert(logCaptor.capture());
            assertEquals(100L, logCaptor.getValue().getUserId());
            assertEquals("提交审核", logCaptor.getValue().getAction());
        }

        @Test
        @DisplayName("用户未登录时不记录")
        void shouldSkipWhenNotLoggedIn() {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(null);

            logService.recordAction("提交审核", "作品", 1L);

            verify(sysLogMapper, never()).insert(any(SysLog.class));
        }
    }
}

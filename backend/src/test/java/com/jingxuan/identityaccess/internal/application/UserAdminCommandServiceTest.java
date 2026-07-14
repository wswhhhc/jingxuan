package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.exception.BusinessException;
import com.jingxuan.identityaccess.api.V1BatchImportResult;
import com.jingxuan.identityaccess.api.V1UserRequest;
import com.jingxuan.modules.userimport.service.AiUserImportService;
import com.jingxuan.service.SysUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("UserAdminCommandService - 批量创建安全边界")
class UserAdminCommandServiceTest {

    private final SysUserService sysUserService = mock(SysUserService.class);
    private final UserAdminCommandService service = new UserAdminCommandService(
            sysUserService,
            mock(AiUserImportService.class)
    );

    @Test
    @DisplayName("单次超过 100 个用户时在执行创建前拒绝")
    void shouldRejectBatchLargerThanOneHundredUsers() {
        List<V1UserRequest> requests = Collections.nCopies(101, null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.batchCreate(requests));

        assertEquals("单次最多导入100个用户", exception.getMessage());
        verify(sysUserService, never()).createUser(any());
    }

    @Test
    @DisplayName("批量创建中的 null 条目返回受控失败而不是 500")
    void shouldReportNullBatchItemWithoutThrowing() {
        V1BatchImportResult result = service.batchCreate(Arrays.asList((V1UserRequest) null));

        assertEquals(0, result.success());
        assertEquals(1, result.failed());
        assertEquals(List.of("第1条: 用户不能为空"), result.errors());
        verify(sysUserService, never()).createUser(any());
    }
}

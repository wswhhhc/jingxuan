package com.jingxuan.controller;

import com.jingxuan.entity.SysUser;
import com.jingxuan.common.Result;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.exception.RateLimitedException;
import com.jingxuan.identityaccess.api.RateLimitService;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.modules.userimport.dto.AiUserImportRequest;
import com.jingxuan.modules.userimport.service.AiUserImportService;
import com.jingxuan.security.JwtUserDetails;
import com.jingxuan.service.SysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SysUserController - 批量创建安全边界")
class SysUserControllerTest {

    private final SysUserService sysUserService = mock(SysUserService.class);
    private final AiUserImportService aiUserImportService = mock(AiUserImportService.class);
    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final SysUserController controller = new SysUserController(
            sysUserService,
            mock(SysRoleMapper.class),
            mock(SysDictMapper.class),
            aiUserImportService,
            rateLimitService
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("单次超过 100 个用户时在执行创建前拒绝")
    void shouldRejectBatchLargerThanOneHundredUsers() {
        List<SysUser> users = IntStream.range(0, 101)
                .mapToObj(index -> new SysUser())
                .toList();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.batchCreate(users));

        assertEquals("单次最多导入100个用户", exception.getMessage());
        verify(sysUserService, never()).createUser(any(SysUser.class));
    }

    @Test
    @DisplayName("批量创建中的 null 条目返回受控失败而不是 500")
    void shouldReportNullBatchItemWithoutThrowing() {
        Result<Map<String, Object>> result = controller.batchCreate(java.util.Arrays.asList((SysUser) null));

        assertEquals(0, result.getData().get("success"));
        assertEquals(1, result.getData().get("failed"));
        assertEquals(List.of("第1条: 用户不能为空"), result.getData().get("errors"));
        verify(sysUserService, never()).createUser(any(SysUser.class));
    }

    @Test
    @DisplayName("AI 导入按当前管理员执行每小时限流")
    void shouldRateLimitAiImportForCurrentAdmin() {
        JwtUserDetails principal = new JwtUserDetails(
                7L, "admin", "管理员", "unused", 3, "ADMIN",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(rateLimitService.consume("ai-user-import", "admin:7", 10, Duration.ofHours(1)))
                .thenReturn(new RateLimitService.Decision(false, 10, 3600));

        RateLimitedException exception = assertThrows(RateLimitedException.class,
                () -> controller.aiParse(new AiUserImportRequest()));

        assertEquals(429, exception.getCode());
        assertEquals(3600, exception.getRetryAfterSeconds());
        verify(aiUserImportService, never()).parse(any(AiUserImportRequest.class));
    }
}

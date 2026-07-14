package com.jingxuan.identityaccess.web;

import com.jingxuan.exception.RateLimitedException;
import com.jingxuan.identityaccess.api.RateLimitService;
import com.jingxuan.identityaccess.api.V1AiUserImportRequest;
import com.jingxuan.identityaccess.internal.application.UserAdminCommandService;
import com.jingxuan.identityaccess.internal.application.UserAdminQueryService;
import com.jingxuan.security.JwtUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("V1UserAdminController - AI 导入安全边界")
class V1UserAdminControllerTest {

    private final UserAdminCommandService commandService = mock(UserAdminCommandService.class);
    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final V1UserAdminController controller = new V1UserAdminController(
            mock(UserAdminQueryService.class),
            commandService,
            rateLimitService
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
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
                () -> controller.aiParse(new V1AiUserImportRequest(List.of())));

        assertEquals(429, exception.getCode());
        assertEquals(3600, exception.getRetryAfterSeconds());
        verify(commandService, never()).aiParse(any());
    }
}

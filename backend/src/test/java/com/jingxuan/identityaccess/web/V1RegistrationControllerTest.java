package com.jingxuan.identityaccess.web;

import com.jingxuan.api.RequestIdFilter;
import com.jingxuan.api.V1ExceptionHandler;
import com.jingxuan.auth.service.RegistrationService;
import com.jingxuan.identityaccess.api.RateLimitService;
import com.jingxuan.security.TrustedProxyClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class V1RegistrationControllerTest {

    private final RegistrationService registrationService = mock(RegistrationService.class);
    private final RateLimitService rateLimits = mock(RateLimitService.class);
    private final TrustedProxyClientIpResolver clientIpResolver = mock(TrustedProxyClientIpResolver.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new V1RegistrationController(
            registrationService, rateLimits, clientIpResolver))
            .setControllerAdvice(new V1ExceptionHandler())
            .addFilters(new RequestIdFilter())
            .build();

    @BeforeEach
    void allowRateLimitsAndResolveClientIp() {
        when(clientIpResolver.resolve(any())).thenReturn("203.0.113.10");
        when(rateLimits.consume(any(), any(), any(Integer.class), any(Duration.class)))
                .thenReturn(new RateLimitService.Decision(true, 1, 60));
    }

    @Test
    void sendsEmailVerificationWithAddressAndTrustedIpLimits() throws Exception {
        mockMvc.perform(post("/api/v1/auth/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Teacher@Example.edu\",\"roleId\":2}"))
                .andExpect(status().isNoContent());

        verify(rateLimits).consume("email-verification-address-minute", "email:teacher@example.edu", 1,
                Duration.ofMinutes(1));
        verify(rateLimits).consume("email-verification-address-hour", "email:teacher@example.edu", 5,
                Duration.ofHours(1));
        verify(rateLimits).consume("email-verification-ip-minute", "ip:203.0.113.10", 1,
                Duration.ofMinutes(1));
        verify(rateLimits).consume("email-verification-ip-hour", "ip:203.0.113.10", 5,
                Duration.ofHours(1));
        verify(registrationService).sendVerificationCode(Map.of("email", "teacher@example.edu", "roleId", 2));
    }

    @Test
    void rateLimitReturnsProblemDetailsWithRetryAfter() throws Exception {
        when(rateLimits.consume(eq("email-verification-address-minute"), any(), any(Integer.class), any(Duration.class)))
                .thenReturn(new RateLimitService.Decision(false, 1, 37));

        mockMvc.perform(post("/api/v1/auth/email-verifications")
                        .header(RequestIdFilter.HEADER, "email-limit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"teacher@example.edu\",\"roleId\":2}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.requestId").value("email-limit"))
                .andExpect(header().string("Retry-After", "37"));
    }

    @Test
    void registersWithV1BoundaryValidationAndReturnsCreatedUser() throws Exception {
        when(registrationService.register(any())).thenReturn(Map.of("id", 9007199254740993L));

        mockMvc.perform(post("/api/v1/auth/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"teacher001","password":"ExamplePass2026","realName":"教师",
                                "email":"Teacher@Example.edu","verifyCode":"123456","roleId":"2"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("9007199254740993"))
                .andExpect(jsonPath("$.roleCode").value("TEACHER"));
    }
}

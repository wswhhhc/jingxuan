package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.identityaccess.api.ChallengePurpose;
import com.jingxuan.identityaccess.api.ChallengeService;
import com.jingxuan.identityaccess.api.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginProtectionServiceTest {

    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final ChallengeService challengeService = mock(ChallengeService.class);
    private final LoginProtectionService service = new LoginProtectionService(rateLimitService, challengeService);

    @BeforeEach
    void allowAllPolicies() {
        when(rateLimitService.inspect(anyString(), anyString(), anyInt(), any(Duration.class)))
                .thenReturn(new RateLimitService.Decision(true, 0, 900));
        when(rateLimitService.consume(anyString(), anyString(), anyInt(), any(Duration.class)))
                .thenReturn(new RateLimitService.Decision(true, 1, 900));
        when(rateLimitService.reset(anyString(), anyString(), anyInt(), any(Duration.class))).thenReturn(true);
    }

    @Test
    void hardLimitOnEitherAccountOrIpRejectsBeforeChallengeOrCredentialVerification() {
        when(rateLimitService.inspect(eq("login-ip"), anyString(), eq(20), any(Duration.class)))
                .thenReturn(new RateLimitService.Decision(false, 20, 42));

        LoginRateLimitedException exception = assertThrows(LoginRateLimitedException.class,
                () -> service.beforeAuthentication("student", "203.0.113.10", null, null));

        assertEquals(429, exception.status());
        assertEquals("RATE_LIMITED", exception.problemCode());
        assertEquals(42, exception.retryAfterSeconds());
        verifyNoInteractions(challengeService);
    }

    @Test
    void fifthPriorFailureOnEitherPolicyRequiresAOneTimeLoginChallenge() {
        when(rateLimitService.inspect(eq("login-account"), anyString(), eq(5), any(Duration.class)))
                .thenReturn(new RateLimitService.Decision(false, 5, 600));

        LoginChallengeRequiredException exception = assertThrows(LoginChallengeRequiredException.class,
                () -> service.beforeAuthentication("student", "203.0.113.10", null, null));

        assertEquals(401, exception.status());
        assertEquals("LOGIN_CHALLENGE_REQUIRED", exception.problemCode());
        verifyNoInteractions(challengeService);
    }

    @Test
    void requiredChallengeIsConsumedAndItsFailureDoesNotReachCredentialAuthentication() {
        when(rateLimitService.inspect(eq("login-account"), anyString(), eq(5), any(Duration.class)))
                .thenReturn(new RateLimitService.Decision(false, 5, 600));
        when(challengeService.verifyAndConsume("AAAAAAAAAAAAAAAAAAAAAA", ChallengePurpose.LOGIN, 7))
                .thenReturn(false);

        LoginChallengeInvalidException exception = assertThrows(LoginChallengeInvalidException.class,
                () -> service.beforeAuthentication("student", "203.0.113.10", "AAAAAAAAAAAAAAAAAAAAAA", 7));

        assertEquals(401, exception.status());
        assertEquals("LOGIN_CHALLENGE_INVALID", exception.problemCode());
        verify(challengeService).verifyAndConsume("AAAAAAAAAAAAAAAAAAAAAA", ChallengePurpose.LOGIN, 7);
    }

    @Test
    void validChallengeLetsTheAttemptContinueAfterTheThreshold() {
        when(rateLimitService.inspect(eq("login-account"), anyString(), eq(5), any(Duration.class)))
                .thenReturn(new RateLimitService.Decision(false, 5, 600));
        when(challengeService.verifyAndConsume("AAAAAAAAAAAAAAAAAAAAAA", ChallengePurpose.LOGIN, 7))
                .thenReturn(true);

        assertDoesNotThrow(
                () -> service.beforeAuthentication("student", "203.0.113.10", "AAAAAAAAAAAAAAAAAAAAAA", 7));
    }

    @Test
    void failedCredentialsIncreaseBothAccountAndIpPoliciesForBothThresholds() {
        service.recordFailure("student", "203.0.113.10");

        verify(rateLimitService).consume(eq("login-account"), anyString(), eq(5), any(Duration.class));
        verify(rateLimitService).consume(eq("login-account"), anyString(), eq(20), any(Duration.class));
        verify(rateLimitService).consume(eq("login-ip"), anyString(), eq(5), any(Duration.class));
        verify(rateLimitService).consume(eq("login-ip"), anyString(), eq(20), any(Duration.class));
    }

    @Test
    void successfulLoginClearsOnlyTheAccountPolicies() {
        service.recordSuccess("student");

        verify(rateLimitService).reset(eq("login-account"), anyString(), eq(5), any(Duration.class));
        verify(rateLimitService).reset(eq("login-account"), anyString(), eq(20), any(Duration.class));
        verify(rateLimitService, never()).reset(eq("login-ip"), anyString(), anyInt(), any(Duration.class));
    }
}

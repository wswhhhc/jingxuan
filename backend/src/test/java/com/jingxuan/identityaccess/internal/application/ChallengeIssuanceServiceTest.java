package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.identityaccess.api.ChallengePurpose;
import com.jingxuan.identityaccess.api.ChallengeService;
import com.jingxuan.identityaccess.api.RateLimitService;
import com.jingxuan.identityaccess.api.RateLimitUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChallengeIssuanceServiceTest {

    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final ChallengeService challengeService = mock(ChallengeService.class);
    private final ChallengeIssuanceService service = new ChallengeIssuanceService(rateLimitService, challengeService);

    @Test
    void issueConsumesTheTrustedIpBudgetBeforeCreatingRedisChallengeState() {
        ChallengeService.IssuedChallenge issued = new ChallengeService.IssuedChallenge(
                "AAAAAAAAAAAAAAAAAAAAAA", "1 + 1 = ?", 300);
        when(rateLimitService.consume("challenge-issue-ip", "ip:203.0.113.10", 10, Duration.ofMinutes(1)))
                .thenReturn(new RateLimitService.Decision(true, 10, 1));
        when(challengeService.issue(ChallengePurpose.LOGIN)).thenReturn(issued);

        assertSame(issued, service.issue(ChallengePurpose.LOGIN, "203.0.113.10"));

        verify(challengeService).issue(ChallengePurpose.LOGIN);
    }

    @Test
    void exhaustedBudgetDoesNotCreateAnyChallengeKey() {
        when(rateLimitService.consume("challenge-issue-ip", "ip:203.0.113.10", 10, Duration.ofMinutes(1)))
                .thenReturn(new RateLimitService.Decision(false, 10, 23));

        ChallengeIssueRateLimitedException exception = assertThrows(ChallengeIssueRateLimitedException.class,
                () -> service.issue(ChallengePurpose.LOGIN, "203.0.113.10"));

        assertEquals(429, exception.status());
        assertEquals(23, exception.retryAfterSeconds());
        verify(challengeService, never()).issue(ChallengePurpose.LOGIN);
    }

    @Test
    void unavailableRateLimitStoreFailsClosedBeforeChallengeCreation() {
        when(rateLimitService.consume("challenge-issue-ip", "ip:203.0.113.10", 10, Duration.ofMinutes(1)))
                .thenThrow(new RateLimitUnavailableException());

        assertThrows(RateLimitUnavailableException.class,
                () -> service.issue(ChallengePurpose.LOGIN, "203.0.113.10"));

        verify(challengeService, never()).issue(ChallengePurpose.LOGIN);
    }
}

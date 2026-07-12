package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.identityaccess.api.ChallengePurpose;
import com.jingxuan.identityaccess.api.ChallengeService;
import com.jingxuan.identityaccess.api.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** 对公开 challenge 发行施加 IP 配额，避免短 TTL key 被无限制造。 */
@Service
@RequiredArgsConstructor
public class ChallengeIssuanceService {

    private static final int LIMIT = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RateLimitService rateLimitService;
    private final ChallengeService challengeService;

    public ChallengeService.IssuedChallenge issue(ChallengePurpose purpose, String clientIp) {
        RateLimitService.Decision decision = rateLimitService.consume(
                "challenge-issue-ip", "ip:" + clientIp, LIMIT, WINDOW);
        if (!decision.allowed()) {
            throw new ChallengeIssueRateLimitedException(decision.retryAfterSeconds());
        }
        return challengeService.issue(purpose);
    }
}

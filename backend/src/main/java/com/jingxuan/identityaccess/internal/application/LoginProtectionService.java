package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.identityaccess.api.ChallengePurpose;
import com.jingxuan.identityaccess.api.ChallengeService;
import com.jingxuan.identityaccess.api.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** 登录失败计数、challenge 与硬限流的身份访问用例。 */
@Service
@RequiredArgsConstructor
public class LoginProtectionService {

    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int CHALLENGE_THRESHOLD = 5;
    private static final int HARD_LIMIT = 20;

    private final RateLimitService rateLimitService;
    private final ChallengeService challengeService;

    /** 在凭据验证前执行，防止硬限流请求消耗密码哈希资源。 */
    public void beforeAuthentication(String account, String clientIp, String challengeId, Integer challengeAnswer) {
        String accountSubject = accountSubject(account);
        String ipSubject = ipSubject(clientIp);
        long retryAfterSeconds = maxRejectedRetryAfter(accountSubject, ipSubject, HARD_LIMIT);
        if (retryAfterSeconds > 0) {
            throw new LoginRateLimitedException(retryAfterSeconds);
        }

        boolean challengeRequired = maxRejectedRetryAfter(accountSubject, ipSubject, CHALLENGE_THRESHOLD) > 0;
        if (!challengeRequired) {
            return;
        }
        if (challengeId == null || challengeAnswer == null) {
            throw new LoginChallengeRequiredException();
        }
        if (!challengeService.verifyAndConsume(challengeId, ChallengePurpose.LOGIN, challengeAnswer)) {
            throw new LoginChallengeInvalidException();
        }
    }

    /** 凭据失败后同时计入账号与 IP；第 N 次失败影响下一次请求。 */
    public void recordFailure(String account, String clientIp) {
        String accountSubject = accountSubject(account);
        String ipSubject = ipSubject(clientIp);
        consume(accountSubject, "login-account", CHALLENGE_THRESHOLD);
        consume(accountSubject, "login-account", HARD_LIMIT);
        consume(ipSubject, "login-ip", CHALLENGE_THRESHOLD);
        consume(ipSubject, "login-ip", HARD_LIMIT);
    }

    /** 成功登录只能清账号失败量，IP 失败量保留至窗口结束。 */
    public void recordSuccess(String account) {
        String accountSubject = accountSubject(account);
        rateLimitService.reset("login-account", accountSubject, CHALLENGE_THRESHOLD, WINDOW);
        rateLimitService.reset("login-account", accountSubject, HARD_LIMIT, WINDOW);
    }

    private long maxRejectedRetryAfter(String accountSubject, String ipSubject, int limit) {
        long accountRetry = rejectedRetryAfter("login-account", accountSubject, limit);
        long ipRetry = rejectedRetryAfter("login-ip", ipSubject, limit);
        return Math.max(accountRetry, ipRetry);
    }

    private long rejectedRetryAfter(String namespace, String subject, int limit) {
        RateLimitService.Decision decision = rateLimitService.inspect(namespace, subject, limit, WINDOW);
        return decision.allowed() ? 0 : decision.retryAfterSeconds();
    }

    private void consume(String subject, String namespace, int limit) {
        rateLimitService.consume(namespace, subject, limit, WINDOW);
    }

    private static String accountSubject(String account) {
        return "account:" + account;
    }

    private static String ipSubject(String clientIp) {
        return "ip:" + clientIp;
    }
}

package com.jingxuan.identityaccess.api;

import java.time.Duration;

/**
 * identity-access 对外提供的通用固定窗口限流能力。
 */
public interface RateLimitService {

    /**
     * 消耗一次配额。第 {@code limit} 次仍允许，后续请求被拒绝。
     */
    Decision consume(String namespace, String subject, int limit, Duration window);

    /**
     * 只读当前窗口；当 {@code count < limit} 时 {@code allowed=true}。
     */
    Decision inspect(String namespace, String subject, int limit, Duration window);

    /**
     * 幂等重置指定策略的当前窗口。参数无效时返回 false，存储不可用时抛出脱敏异常。
     */
    boolean reset(String namespace, String subject, int limit, Duration window);

    /**
     * 为过渡期调用者保留的布尔兼容入口。
     */
    default boolean tryAcquire(String namespace, String subject, int limit, Duration window) {
        return consume(namespace, subject, limit, window).allowed();
    }

    /**
     * @param allowed 本次请求是否允许
     * @param count 当前窗口内已计入的次数
     * @param retryAfterSeconds 当前窗口剩余秒数，最少为 1
     */
    record Decision(boolean allowed, long count, long retryAfterSeconds) {

        public Decision {
            if (count < 0) {
                throw new IllegalArgumentException("count 不能为负数");
            }
            if (retryAfterSeconds < 1) {
                throw new IllegalArgumentException("retryAfterSeconds 必须至少为 1");
            }
        }
    }
}

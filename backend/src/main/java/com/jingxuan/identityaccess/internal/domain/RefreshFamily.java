package com.jingxuan.identityaccess.internal.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/** Refresh token family 的绝对生命周期与当前令牌状态。 */
public record RefreshFamily(
        String id,
        Long userId,
        String username,
        String role,
        boolean rememberMe,
        String currentTokenHash,
        Status status,
        Instant expiresAt
) {

    private static final Duration DEFAULT_LIFETIME = Duration.ofHours(8);
    private static final Duration REMEMBERED_LIFETIME = Duration.ofDays(30);
    private static final Pattern SHA_256_HEX = Pattern.compile("[0-9a-f]{64}");

    public RefreshFamily {
        requireText(id, "family id");
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("user id 无效");
        }
        requireText(username, "username");
        requireText(role, "role");
        if (currentTokenHash == null || !SHA_256_HEX.matcher(currentTokenHash).matches()) {
            throw new IllegalArgumentException("refresh token hash 无效");
        }
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public static RefreshFamily issue(String id, Long userId, String username, String role,
                                      boolean rememberMe, String currentTokenHash, Instant issuedAt) {
        Objects.requireNonNull(issuedAt, "issuedAt");
        Duration lifetime = rememberMe ? REMEMBERED_LIFETIME : DEFAULT_LIFETIME;
        return new RefreshFamily(id, userId, username, role, rememberMe, currentTokenHash,
                Status.ACTIVE, issuedAt.plus(lifetime));
    }

    public RefreshFamily rotateTo(String replacementHash, Instant rotatedAt) {
        Objects.requireNonNull(rotatedAt, "rotatedAt");
        if (status != Status.ACTIVE || !rotatedAt.isBefore(expiresAt)) {
            throw new IllegalStateException("refresh family 已失效");
        }
        return new RefreshFamily(id, userId, username, role, rememberMe, replacementHash,
                Status.ACTIVE, expiresAt);
    }

    public RefreshFamily revoke() {
        return new RefreshFamily(id, userId, username, role, rememberMe, currentTokenHash,
                Status.REVOKED, expiresAt);
    }

    public long remainingSeconds(Instant now) {
        Objects.requireNonNull(now, "now");
        if (!now.isBefore(expiresAt)) {
            return 0;
        }
        return Duration.between(now, expiresAt).toSeconds();
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " 无效");
        }
    }

    public enum Status {
        ACTIVE,
        REVOKED
    }
}

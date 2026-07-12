package com.jingxuan.identityaccess.internal.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshFamilyTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-07-11T10:00:00Z");
    private static final String TOKEN_HASH = "a".repeat(64);
    private static final String REPLACEMENT_HASH = "b".repeat(64);

    @Test
    void defaultFamilyExpiresAfterEightHours() {
        RefreshFamily family = RefreshFamily.issue(
                "family-id", 7L, "student", "STUDENT", false, TOKEN_HASH, ISSUED_AT);

        assertEquals(ISSUED_AT.plus(Duration.ofHours(8)), family.expiresAt());
        assertEquals(Duration.ofHours(8).toSeconds(), family.remainingSeconds(ISSUED_AT));
    }

    @Test
    void rememberedFamilyExpiresAfterThirtyDays() {
        RefreshFamily family = RefreshFamily.issue(
                "family-id", 7L, "student", "STUDENT", true, TOKEN_HASH, ISSUED_AT);

        assertEquals(ISSUED_AT.plus(Duration.ofDays(30)), family.expiresAt());
        assertEquals(Duration.ofDays(30).toSeconds(), family.remainingSeconds(ISSUED_AT));
    }

    @Test
    void rotationKeepsTheAbsoluteDeadline() {
        RefreshFamily family = RefreshFamily.issue(
                "family-id", 7L, "student", "STUDENT", false, TOKEN_HASH, ISSUED_AT);

        RefreshFamily rotated = family.rotateTo(REPLACEMENT_HASH, ISSUED_AT.plus(Duration.ofHours(2)));

        assertEquals(family.expiresAt(), rotated.expiresAt());
        assertEquals(REPLACEMENT_HASH, rotated.currentTokenHash());
        assertEquals(Duration.ofHours(6).toSeconds(), rotated.remainingSeconds(ISSUED_AT.plus(Duration.ofHours(2))));
    }

    @Test
    void expiredFamilyCannotRotate() {
        RefreshFamily family = RefreshFamily.issue(
                "family-id", 7L, "student", "STUDENT", false, TOKEN_HASH, ISSUED_AT);

        assertThrows(IllegalStateException.class,
                () -> family.rotateTo(REPLACEMENT_HASH, family.expiresAt()));
    }

    @Test
    void revokeTransitionsTheFamilyAndPreservesItsAbsoluteDeadline() {
        RefreshFamily family = RefreshFamily.issue(
                "family-id", 7L, "student", "STUDENT", false, TOKEN_HASH, ISSUED_AT);

        RefreshFamily revoked = family.revoke();

        assertEquals(RefreshFamily.Status.REVOKED, revoked.status());
        assertEquals(TOKEN_HASH, revoked.currentTokenHash());
        assertEquals(family.expiresAt(), revoked.expiresAt());
    }

    @Test
    void revokedFamilyCannotRotate() {
        RefreshFamily family = RefreshFamily.issue(
                "family-id", 7L, "student", "STUDENT", false, TOKEN_HASH, ISSUED_AT).revoke();

        assertThrows(IllegalStateException.class,
                () -> family.rotateTo(REPLACEMENT_HASH, ISSUED_AT.plusSeconds(1)));
    }

    @Test
    void refreshTokenHashMustBeLowercaseSha256Hex() {
        assertThrows(IllegalArgumentException.class, () -> RefreshFamily.issue(
                "family-id", 7L, "student", "STUDENT", false, "a".repeat(63), ISSUED_AT));
        assertThrows(IllegalArgumentException.class, () -> RefreshFamily.issue(
                "family-id", 7L, "student", "STUDENT", false, "A".repeat(64), ISSUED_AT));
        assertThrows(IllegalArgumentException.class, () -> RefreshFamily.issue(
                "family-id", 7L, "student", "STUDENT", false, "g".repeat(64), ISSUED_AT));
    }
}

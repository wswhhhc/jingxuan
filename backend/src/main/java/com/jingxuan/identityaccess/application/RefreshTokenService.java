package com.jingxuan.identityaccess.application;

/** v1 refresh token 会话用例。 */
public interface RefreshTokenService {

    IssuedRefreshToken issue(Long userId, String username, String role, boolean rememberMe);

    RotatedRefreshToken rotate(String refreshToken);

    void revoke(String refreshToken);

    void revokeAll(Long userId);

    record IssuedRefreshToken(String token, long expiresIn) {
    }

    record RotatedRefreshToken(Long userId, String username, String role,
                               IssuedRefreshToken replacement) {
    }
}

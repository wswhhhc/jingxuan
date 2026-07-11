package com.jingxuan.identityaccess.api;

import com.jingxuan.auth.model.LoginResponse;

/** v1 登录响应。 */
public record V1LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Long refreshExpiresIn,
        V1UserInfo user
) {
    public static V1LoginResponse from(LoginResponse source, String accessToken,
                                       String refreshToken, long accessExpiresIn, long refreshExpiresIn) {
        return new V1LoginResponse(accessToken, refreshToken, source.getTokenType(), accessExpiresIn,
                refreshExpiresIn,
                source.getUserInfo() == null ? null : V1UserInfo.from(source.getUserInfo()));
    }
}

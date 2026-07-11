package com.jingxuan.identityaccess.api;

import com.jingxuan.auth.model.LoginResponse;

/** v1 登录响应。 */
public record V1LoginResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        V1UserInfo user
) {
    public static V1LoginResponse from(LoginResponse source) {
        return new V1LoginResponse(source.getToken(), source.getTokenType(), source.getExpiresIn(),
                source.getUserInfo() == null ? null : V1UserInfo.from(source.getUserInfo()));
    }
}

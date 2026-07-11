package com.jingxuan.identityaccess.api;

import jakarta.validation.constraints.NotBlank;

/** refresh token 轮换请求。 */
public record V1RefreshRequest(@NotBlank String refreshToken) {
}

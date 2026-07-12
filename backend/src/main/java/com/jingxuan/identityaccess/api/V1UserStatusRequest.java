package com.jingxuan.identityaccess.api;

import jakarta.validation.constraints.NotNull;

/** v1 更新用户状态请求。 */
public record V1UserStatusRequest(
        @NotNull Integer status
) {
}

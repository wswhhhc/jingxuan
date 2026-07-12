package com.jingxuan.identityaccess.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** v1 创建/更新用户请求。 */
public record V1UserRequest(
        @NotBlank String username,
        String password,
        @NotBlank String realName,
        @NotNull Integer roleId,
        Long classId,
        String phone,
        String email
) {
}

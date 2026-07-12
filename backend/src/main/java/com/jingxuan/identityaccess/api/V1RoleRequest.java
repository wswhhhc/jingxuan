package com.jingxuan.identityaccess.api;

import jakarta.validation.constraints.NotBlank;

/** v1 创建/更新角色请求。 */
public record V1RoleRequest(
        @NotBlank String roleName,
        @NotBlank String roleCode,
        String description
) {
}

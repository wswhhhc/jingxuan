package com.jingxuan.identityaccess.api;

import jakarta.validation.constraints.NotBlank;

/** v1 创建/更新菜单请求。 */
public record V1MenuRequest(
        @NotBlank String menuName,
        String parentId,
        String path,
        String permission,
        String type,
        String icon,
        Integer sort
) {
}

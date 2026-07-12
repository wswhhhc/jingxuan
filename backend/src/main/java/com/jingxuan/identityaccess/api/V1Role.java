package com.jingxuan.identityaccess.api;

import com.jingxuan.entity.SysRole;

/** v1 角色 DTO。 */
public record V1Role(
        String id,
        String roleName,
        String roleCode,
        String description,
        String createTime,
        String updateTime
) {
    public static V1Role from(SysRole role) {
        return new V1Role(
                role.getId() != null ? role.getId().toString() : null,
                role.getRoleName(),
                role.getRoleCode(),
                role.getDescription(),
                role.getCreateTime() != null ? role.getCreateTime().toString() : null,
                role.getUpdateTime() != null ? role.getUpdateTime().toString() : null
        );
    }
}

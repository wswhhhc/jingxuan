package com.jingxuan.identityaccess.api;

import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.UserStatusEnum;

/** v1 用户 DTO。 */
public record V1User(
        String id,
        String username,
        String realName,
        Integer roleId,
        String roleName,
        Long classId,
        String className,
        String avatar,
        String phone,
        String email,
        Integer status,
        Boolean firstLogin,
        String createTime,
        String updateTime
) {
    public static V1User from(SysUser user, String roleName, String className) {
        return new V1User(
                user.getId() != null ? user.getId().toString() : null,
                user.getUsername(),
                user.getRealName(),
                user.getRoleId(),
                roleName,
                user.getClassId(),
                className,
                user.getAvatar(),
                user.getPhone(),
                user.getEmail(),
                user.getStatus() != null ? user.getStatus().getValue() : null,
                user.getFirstLogin(),
                user.getCreateTime() != null ? user.getCreateTime().toString() : null,
                user.getUpdateTime() != null ? user.getUpdateTime().toString() : null
        );
    }
}

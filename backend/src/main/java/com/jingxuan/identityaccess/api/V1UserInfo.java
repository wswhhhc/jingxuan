package com.jingxuan.identityaccess.api;

import com.jingxuan.auth.model.UserInfoVO;

/** v1 用户视图：所有 ID 以不透明字符串传输。 */
public record V1UserInfo(
        String id,
        String username,
        String realName,
        Integer roleId,
        String roleCode,
        String roleName,
        String avatar,
        Boolean firstLogin,
        String classId,
        String className,
        String phone,
        String email
) {
    public static V1UserInfo from(UserInfoVO source) {
        return new V1UserInfo(
                asString(source.getId()), source.getUsername(), source.getRealName(), source.getRoleId(),
                source.getRoleCode(), source.getRoleName(), source.getAvatar(), source.getFirstLogin(),
                asString(source.getClassId()), source.getClassName(), source.getPhone(), source.getEmail());
    }

    private static String asString(Long value) {
        return value == null ? null : value.toString();
    }
}

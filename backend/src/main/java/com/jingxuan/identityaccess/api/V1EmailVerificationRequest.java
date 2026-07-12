package com.jingxuan.identityaccess.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 申请注册邮箱验证码的 v1 输入。 */
public record V1EmailVerificationRequest(
        @NotBlank(message = "请输入邮箱地址") @Email(message = "邮箱格式不正确") String email,
        @NotNull(message = "请选择有效的角色") @Pattern(regexp = "1|2", message = "请选择有效的角色") String roleId
) {
    public String normalizedEmail() {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public Integer parsedRoleId() {
        return Integer.valueOf(roleId);
    }
}

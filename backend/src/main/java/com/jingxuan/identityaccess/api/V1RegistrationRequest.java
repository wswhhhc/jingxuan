package com.jingxuan.identityaccess.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** v1 自助注册输入，ID 一律以字符串承载。 */
public record V1RegistrationRequest(
        @NotBlank(message = "请输入用户名（学号/工号）") @Size(max = 64, message = "用户名不能超过64个字符") String username,
        @NotBlank(message = "请输入密码") @Size(min = 8, max = 128, message = "密码长度必须为8到128个字符") String password,
        @NotBlank(message = "请输入真实姓名") @Size(max = 64, message = "真实姓名不能超过64个字符") String realName,
        @NotBlank(message = "请输入邮箱地址") @Email(message = "邮箱格式不正确") String email,
        @NotBlank(message = "请输入验证码") @Pattern(regexp = "[0-9]{6}", message = "验证码格式不正确") String verifyCode,
        @NotNull(message = "请选择有效的角色") @Pattern(regexp = "1|2", message = "请选择有效的角色") String roleId,
        String classId
) {
    public String normalizedEmail() {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public Integer parsedRoleId() {
        return Integer.valueOf(roleId);
    }
}

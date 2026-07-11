package com.jingxuan.auth.controller;

import com.jingxuan.auth.model.LoginRequest;
import com.jingxuan.auth.model.LoginResponse;
import com.jingxuan.auth.model.UserInfoVO;
import com.jingxuan.auth.service.AuthService;
import com.jingxuan.auth.service.RegistrationService;
import com.jingxuan.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口
 */
@Tag(name = "认证管理", description = "登录、登出、获取用户信息")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;

    @Operation(summary = "检查当前用户是否需要修改密码（首次登录）")
    @GetMapping("/check-first-login")
    public Result<Map<String, Boolean>> checkFirstLogin() {
        boolean firstLogin = authService.checkFirstLogin();
        return Result.ok(Map.of("firstLogin", firstLogin));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.ok("登录成功", response);
    }

    @Operation(summary = "获取当前用户信息（兼容前端 /auth/user-info）")
    @GetMapping({"/me", "/user-info"})
    public Result<UserInfoVO> getCurrentUser() {
        UserInfoVO userInfo = authService.getCurrentUserInfo();
        return Result.ok(userInfo);
    }

    @Operation(summary = "修改密码（兼容前端 /auth/change-password）")
    @PutMapping({"/password", "/change-password"})
    public Result<Void> changePassword(@Valid @RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return Result.fail("原密码和新密码不能为空");
        }
        authService.changePassword(oldPassword, newPassword);
        return Result.ok();
    }

    @Operation(summary = "更新个人信息（头像/邮箱/手机号）")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody Map<String, Object> body) {
        authService.updateProfile(body);
        return Result.ok();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }
}

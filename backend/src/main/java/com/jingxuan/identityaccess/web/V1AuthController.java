package com.jingxuan.identityaccess.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.auth.model.LoginRequest;
import com.jingxuan.auth.service.AuthService;
import com.jingxuan.identityaccess.api.V1LoginResponse;
import com.jingxuan.identityaccess.api.V1UserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** v1 身份访问入口，业务逻辑委托给现有认证用例。 */
@V1Api
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "v1 认证", description = "登录、注销和当前用户")
public class V1AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ResponseEntity<V1LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(V1LoginResponse.from(authService.login(request)));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户")
    public ResponseEntity<V1UserInfo> me() {
        return ResponseEntity.ok(V1UserInfo.from(authService.getCurrentUserInfo()));
    }

    @PostMapping("/logout")
    @Operation(summary = "注销当前会话")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }
}

package com.jingxuan.identityaccess.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.auth.model.LoginRequest;
import com.jingxuan.auth.service.AuthService;
import com.jingxuan.identityaccess.api.V1RefreshRequest;
import com.jingxuan.identityaccess.application.RefreshTokenService;
import com.jingxuan.security.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ResponseEntity<V1LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var legacy = authService.login(request);
        var user = legacy.getUserInfo();
        var refresh = refreshTokenService.issue(user.getId(), user.getUsername(), user.getRoleCode(),
                Boolean.TRUE.equals(request.getRememberMe()));
        String accessToken = jwtTokenProvider.generateV1AccessToken(user.getId(), user.getUsername(), user.getRoleCode());
        return ResponseEntity.ok(V1LoginResponse.from(legacy, accessToken, refresh.token(),
                Math.max(jwtTokenProvider.getRemainingValidity(accessToken) / 1000, 0), refresh.expiresIn()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "轮换刷新令牌")
    public ResponseEntity<V1LoginResponse> refresh(@Valid @RequestBody V1RefreshRequest request) {
        var rotated = refreshTokenService.rotate(request.refreshToken());
        String accessToken = jwtTokenProvider.generateV1AccessToken(rotated.userId(), rotated.username(), rotated.role());
        var user = new V1UserInfo(rotated.userId().toString(), rotated.username(), null, null,
                rotated.role(), null, null, null, null, null, null, null);
        var response = new V1LoginResponse(accessToken, rotated.replacement().token(), "Bearer",
                Math.max(jwtTokenProvider.getRemainingValidity(accessToken) / 1000, 0),
                rotated.replacement().expiresIn(), user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户")
    public ResponseEntity<V1UserInfo> me() {
        return ResponseEntity.ok(V1UserInfo.from(authService.getCurrentUserInfo()));
    }

    @PostMapping("/logout")
    @Operation(summary = "注销当前会话")
    public ResponseEntity<Void> logout(@RequestBody(required = false) V1RefreshRequest request) {
        authService.logout();
        if (request != null) {
            refreshTokenService.revoke(request.refreshToken());
        }
        return ResponseEntity.noContent().build();
    }
}

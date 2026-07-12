package com.jingxuan.identityaccess.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.auth.service.RegistrationService;
import com.jingxuan.identityaccess.api.EmailVerificationRateLimitedException;
import com.jingxuan.identityaccess.api.RateLimitService;
import com.jingxuan.identityaccess.api.V1EmailVerificationRequest;
import com.jingxuan.identityaccess.api.V1RegistrationRequest;
import com.jingxuan.identityaccess.api.V1UserInfo;
import com.jingxuan.security.TrustedProxyClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

/** v1 注册前置操作。 */
@V1Api
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class V1RegistrationController {

    private static final int PER_MINUTE_LIMIT = 1;
    private static final int PER_HOUR_LIMIT = 5;

    private final RegistrationService registrationService;
    private final RateLimitService rateLimits;
    private final TrustedProxyClientIpResolver clientIpResolver;

    @PostMapping("/email-verifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "发送注册邮箱验证码")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "验证码已发送"),
            @ApiResponse(responseCode = "429", description = "邮箱或地址请求超限"),
            @ApiResponse(responseCode = "503", description = "限流存储不可用")
    })
    public void sendEmailVerification(@Valid @RequestBody V1EmailVerificationRequest request,
                                      HttpServletRequest servletRequest) {
        String email = request.normalizedEmail();
        consume("email-verification-address-minute", "email:" + email, PER_MINUTE_LIMIT, Duration.ofMinutes(1));
        consume("email-verification-address-hour", "email:" + email, PER_HOUR_LIMIT, Duration.ofHours(1));
        String clientIp = clientIpResolver.resolve(servletRequest);
        consume("email-verification-ip-minute", "ip:" + clientIp, PER_MINUTE_LIMIT, Duration.ofMinutes(1));
        consume("email-verification-ip-hour", "ip:" + clientIp, PER_HOUR_LIMIT, Duration.ofHours(1));
        registrationService.sendVerificationCode(Map.of("email", email, "roleId", request.parsedRoleId()));
    }

    @PostMapping("/registrations")
    @Operation(summary = "自助注册学生或教师账号")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "注册成功；教师需等待管理员审批"),
            @ApiResponse(responseCode = "422", description = "输入校验失败")
    })
    public ResponseEntity<V1UserInfo> register(@Valid @RequestBody V1RegistrationRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", request.username());
        body.put("password", request.password());
        body.put("realName", request.realName());
        body.put("email", request.normalizedEmail());
        body.put("verifyCode", request.verifyCode());
        body.put("roleId", request.parsedRoleId());
        if (request.classId() != null) {
            body.put("classId", request.classId());
        }
        Map<String, Object> registered = registrationService.register(body);
        String roleCode = request.parsedRoleId() == 1 ? "STUDENT" : "TEACHER";
        V1UserInfo user = new V1UserInfo(
                String.valueOf(registered.get("id")), request.username().trim().toLowerCase(java.util.Locale.ROOT),
                request.realName().trim(), request.parsedRoleId(), roleCode, null, null, false,
                request.classId(), null, null, request.normalizedEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    private void consume(String namespace, String subject, int limit, Duration window) {
        RateLimitService.Decision decision = rateLimits.consume(namespace, subject, limit, window);
        if (!decision.allowed()) {
            throw new EmailVerificationRateLimitedException(decision.retryAfterSeconds());
        }
    }
}

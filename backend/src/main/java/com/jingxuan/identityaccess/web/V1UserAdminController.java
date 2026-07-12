package com.jingxuan.identityaccess.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.api.V1Page;
import com.jingxuan.exception.RateLimitedException;
import com.jingxuan.identityaccess.api.RateLimitService;
import com.jingxuan.identityaccess.api.V1AiUserImportRequest;
import com.jingxuan.identityaccess.api.V1AiUserImportResponse;
import com.jingxuan.identityaccess.api.V1BatchImportResult;
import com.jingxuan.identityaccess.api.V1User;
import com.jingxuan.identityaccess.api.V1UserRequest;
import com.jingxuan.identityaccess.api.V1UserStatusRequest;
import com.jingxuan.identityaccess.internal.application.UserAdminCommandService;
import com.jingxuan.identityaccess.internal.application.UserAdminQueryService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/** v1 管理端用户管理 API — 委托给内部应用用例。 */
@V1Api
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "v1 用户管理", description = "管理员维护用户账号")
public class V1UserAdminController {

    private static final int AI_IMPORT_HOURLY_LIMIT = 10;
    private static final Duration AI_IMPORT_LIMIT_WINDOW = Duration.ofHours(1);

    private final UserAdminQueryService userAdminQueryService;
    private final UserAdminCommandService userAdminCommandService;
    private final RateLimitService rateLimitService;

    @GetMapping
    @Operation(summary = "用户列表（分页）")
    public V1Page<V1User> list(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Integer roleId,
                               @RequestParam(required = false) Integer status) {
        return userAdminQueryService.listUsers(page, size, keyword, roleId, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建用户")
    @ApiResponse(responseCode = "201", description = "用户已创建")
    public void create(@Valid @RequestBody V1UserRequest request) {
        userAdminCommandService.createUser(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "用户详情")
    public V1User getById(@PathVariable String id) {
        Long userId = V1Ids.parse(id, "id");
        return userAdminQueryService.getUserById(userId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑用户")
    public void update(@PathVariable String id, @Valid @RequestBody V1UserRequest request) {
        Long userId = V1Ids.parse(id, "id");
        userAdminCommandService.updateUser(userId, request);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新用户状态")
    @ApiResponse(responseCode = "200", description = "状态已更新")
    public void updateStatus(@PathVariable String id, @Valid @RequestBody V1UserStatusRequest request) {
        Long userId = V1Ids.parse(id, "id");
        userAdminCommandService.updateStatus(userId, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除用户（逻辑删除）")
    @ApiResponse(responseCode = "204", description = "用户已删除")
    public void delete(@PathVariable String id) {
        Long userId = V1Ids.parse(id, "id");
        userAdminCommandService.deleteUser(userId);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量导入用户")
    public V1BatchImportResult batchCreate(@RequestBody List<@Valid V1UserRequest> requests) {
        return userAdminCommandService.batchCreate(requests);
    }

    @PostMapping("/batch/ai-parse")
    @Operation(summary = "AI 解析批量导入用户")
    public V1AiUserImportResponse aiParse(@RequestBody V1AiUserImportRequest request) {
        Long adminId = SecurityUtils.requireCurrentUserId();
        RateLimitService.Decision decision = rateLimitService.consume(
                "ai-user-import", "admin:" + adminId, AI_IMPORT_HOURLY_LIMIT, AI_IMPORT_LIMIT_WINDOW);
        if (!decision.allowed()) {
            throw new RateLimitedException("AI 导入请求过于频繁，请一小时后再试",
                    decision.retryAfterSeconds());
        }
        return userAdminCommandService.aiParse(request);
    }
}

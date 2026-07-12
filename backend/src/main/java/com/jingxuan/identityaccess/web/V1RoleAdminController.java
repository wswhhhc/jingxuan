package com.jingxuan.identityaccess.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.api.V1Page;
import com.jingxuan.identityaccess.api.V1Role;
import com.jingxuan.identityaccess.api.V1RoleRequest;
import com.jingxuan.identityaccess.internal.application.RoleAdminCommandService;
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

import java.util.List;

/** v1 管理端角色管理 API — 委托给内部应用用例。 */
@V1Api
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "v1 角色管理", description = "管理员维护角色")
public class V1RoleAdminController {

    private final RoleAdminCommandService roleAdminCommandService;

    @GetMapping
    @Operation(summary = "角色列表（分页）")
    public V1Page<V1Role> list(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false, defaultValue = "false") Boolean excludeSystem) {
        return roleAdminCommandService.listRoles(page, size, Boolean.TRUE.equals(excludeSystem));
    }

    @GetMapping("/{id}")
    @Operation(summary = "角色详情")
    public V1Role getById(@PathVariable String id) {
        Long roleId = V1Ids.parse(id, "id");
        return roleAdminCommandService.getRoleById(roleId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增角色")
    @ApiResponse(responseCode = "201", description = "角色已创建")
    public void create(@Valid @RequestBody V1RoleRequest request) {
        roleAdminCommandService.createRole(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑角色")
    public void update(@PathVariable String id, @Valid @RequestBody V1RoleRequest request) {
        Long roleId = V1Ids.parse(id, "id");
        roleAdminCommandService.updateRole(roleId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除角色")
    @ApiResponse(responseCode = "204", description = "角色已删除")
    public void delete(@PathVariable String id) {
        Long roleId = V1Ids.parse(id, "id");
        roleAdminCommandService.deleteRole(roleId);
    }

    @GetMapping("/{id}/menus")
    @Operation(summary = "获取角色已分配的菜单 ID 列表")
    public List<Long> getMenuIds(@PathVariable String id) {
        Long roleId = V1Ids.parse(id, "id");
        return roleAdminCommandService.getMenuIds(roleId);
    }

    @PutMapping("/{id}/menus")
    @Operation(summary = "为角色分配菜单")
    public void assignMenus(@PathVariable String id, @RequestBody List<Long> menuIds) {
        Long roleId = V1Ids.parse(id, "id");
        roleAdminCommandService.assignMenus(roleId, menuIds);
    }
}

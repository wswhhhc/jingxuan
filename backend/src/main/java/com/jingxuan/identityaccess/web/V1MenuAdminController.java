package com.jingxuan.identityaccess.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.identityaccess.api.V1Menu;
import com.jingxuan.identityaccess.api.V1MenuRequest;
import com.jingxuan.identityaccess.internal.application.MenuAdminCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** v1 管理端菜单管理 API — 委托给内部应用用例。 */
@V1Api
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "v1 菜单管理", description = "管理员维护菜单权限")
public class V1MenuAdminController {

    private final MenuAdminCommandService menuAdminCommandService;

    @GetMapping("/tree")
    @Operation(summary = "获取菜单树")
    public List<V1Menu> getTree() {
        return menuAdminCommandService.getMenuTree();
    }

    @GetMapping("/{id}")
    @Operation(summary = "菜单详情")
    public V1Menu getById(@PathVariable String id) {
        Long menuId = V1Ids.parse(id, "id");
        return menuAdminCommandService.getMenuById(menuId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增菜单")
    @ApiResponse(responseCode = "201", description = "菜单已创建")
    public void create(@Valid @RequestBody V1MenuRequest request) {
        menuAdminCommandService.createMenu(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑菜单")
    public void update(@PathVariable String id, @Valid @RequestBody V1MenuRequest request) {
        Long menuId = V1Ids.parse(id, "id");
        menuAdminCommandService.updateMenu(menuId, request);
    }
}

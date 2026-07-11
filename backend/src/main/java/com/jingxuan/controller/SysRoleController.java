package com.jingxuan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.SysRole;
import com.jingxuan.entity.SysMenu;
import com.jingxuan.enums.RoleEnum;
import com.jingxuan.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @Operation(summary = "分页查询角色列表")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<SysRole>> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false, defaultValue = "false") Boolean excludeSystem) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .ne(Boolean.TRUE.equals(excludeSystem), SysRole::getRoleCode, RoleEnum.ADMIN.getAuthority())
                .orderByAsc(SysRole::getId);
        Page<SysRole> mpPage = sysRoleService.page(new Page<>(page, size), wrapper);
        return Result.ok(PageResult.of(mpPage.getRecords(), mpPage.getTotal(), page, size));
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.ok(sysRoleService.getById(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> create(@RequestBody SysRole role) {
        sysRoleService.save(role);
        return Result.ok();
    }

    @Operation(summary = "编辑角色")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        sysRoleService.updateById(role);
        return Result.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        sysRoleService.removeById(id);
        return Result.ok();
    }

    @Operation(summary = "获取角色已分配的菜单ID列表")
    @GetMapping("/{id}/menus")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Long>> getMenuIds(@PathVariable Long id) {
        return Result.ok(sysRoleService.getMenuIdsByRoleId(id));
    }

    @Operation(summary = "为角色分配菜单")
    @PutMapping("/{id}/menus")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        sysRoleService.assignMenus(id, menuIds);
        return Result.ok();
    }
}

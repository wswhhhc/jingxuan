package com.jingxuan.controller;

import com.jingxuan.common.Result;
import com.jingxuan.entity.SysMenu;
import com.jingxuan.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/admin/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @Operation(summary = "获取菜单树")
    @GetMapping("/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SysMenu>> getTree() {
        return Result.ok(sysMenuService.getMenuTree());
    }

    @Operation(summary = "菜单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysMenu> getById(@PathVariable Long id) {
        return Result.ok(sysMenuService.getById(id));
    }

    @Operation(summary = "编辑菜单")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysMenu menu) {
        menu.setId(id);
        sysMenuService.updateById(menu);
        return Result.ok();
    }
}

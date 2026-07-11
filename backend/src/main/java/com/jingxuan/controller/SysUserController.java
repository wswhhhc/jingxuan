package com.jingxuan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.dto.UserVO;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysRole;
import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.modules.userimport.dto.AiUserImportRequest;
import com.jingxuan.modules.userimport.dto.AiUserImportResponse;
import com.jingxuan.modules.userimport.service.AiUserImportService;
import com.jingxuan.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理接口（管理员）
 */
@Tag(name = "用户管理", description = "管理员维护用户账号")
@RestController
@RequestMapping({"/admin/users", "/users"})
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;
    private final SysRoleMapper sysRoleMapper;
    private final SysDictMapper sysDictMapper;
    private final AiUserImportService aiUserImportService;

    @Operation(summary = "用户列表")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<UserVO>> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer roleId,
                                           @RequestParam(required = false) Integer status) {
        UserStatusEnum statusEnum = status != null ? UserStatusEnum.of(status) : null;
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getRealName, keyword))
                .eq(roleId != null, SysUser::getRoleId, roleId)
                .eq(statusEnum != null, SysUser::getStatus, statusEnum)
                .orderByDesc(SysUser::getCreateTime);
        var mpPage = sysUserService.page(new Page<>(page, size), wrapper);
        // 批量查询角色名称和班级名称
        Map<Long, String> roleMap = sysRoleMapper.selectList(null).stream()
                .collect(Collectors.toMap(r -> ((Number) r.getId()).longValue(), SysRole::getRoleName, (a, b) -> a));
        Map<Long, String> classMap = sysDictMapper.selectList(null).stream()
                .filter(d -> "class".equals(d.getDictType()))
                .collect(Collectors.toMap(d -> ((Number) d.getId()).longValue(), SysDict::getDictLabel, (a, b) -> a));

        List<UserVO> vos = mpPage.getRecords().stream()
                .map(u -> {
                    String roleName = u.getRoleId() != null ? roleMap.getOrDefault(((Number) u.getRoleId()).longValue(), "") : "";
                    String className = u.getClassId() != null ? classMap.getOrDefault(((Number) u.getClassId()).longValue(), "") : "";
                    UserVO vo = UserVO.from(u, roleName, className);
                    vo.setPassword(null);
                    return vo;
                })
                .collect(Collectors.toList());
        return Result.ok(PageResult.of(vos, mpPage.getTotal(), page, size));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> create(@RequestBody SysUser user) {
        sysUserService.createUser(user);
        return Result.ok();
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        sysUserService.updateUser(user);
        return Result.ok();
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        sysUserService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除用户（逻辑删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return Result.ok();
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user != null) user.setPassword(null);
        return Result.ok(user);
    }

    @Operation(summary = "批量导入用户")
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> batchCreate(@RequestBody List<SysUser> users) {
        int success = 0;
        List<String> errors = new ArrayList<>();
        for (SysUser user : users) {
            try {
                sysUserService.createUser(user);
                success++;
            } catch (Exception e) {
                errors.add(user.getUsername() + ": " + e.getMessage());
            }
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", success);
        result.put("failed", errors.size());
        result.put("errors", errors);
        return Result.ok(result);
    }

    @Operation(summary = "AI 解析批量导入用户")
    @PostMapping("/batch/ai-parse")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AiUserImportResponse> aiParse(@RequestBody AiUserImportRequest request) {
        return Result.ok(aiUserImportService.parse(request));
    }
}

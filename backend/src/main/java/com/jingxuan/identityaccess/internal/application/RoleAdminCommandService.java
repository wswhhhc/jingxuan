package com.jingxuan.identityaccess.internal.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.api.V1Page;
import com.jingxuan.api.V1PageInfo;
import com.jingxuan.entity.SysRole;
import com.jingxuan.enums.RoleEnum;
import com.jingxuan.identityaccess.api.V1Role;
import com.jingxuan.identityaccess.api.V1RoleRequest;
import com.jingxuan.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 角色管理命令/查询用例 — 委托给旧 SysRoleService。 */
@Service
@RequiredArgsConstructor
public class RoleAdminCommandService {

    private final SysRoleService sysRoleService;

    public V1Page<V1Role> listRoles(int page, int size, boolean excludeSystem) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .ne(excludeSystem, SysRole::getRoleCode, RoleEnum.ADMIN.getAuthority())
                .orderByAsc(SysRole::getId);
        Page<SysRole> mpPage = sysRoleService.page(new Page<>(page, size), wrapper);
        List<V1Role> items = mpPage.getRecords().stream()
                .map(V1Role::from)
                .toList();
        return new V1Page<>(items, V1PageInfo.of(page, size, mpPage.getTotal()));
    }

    public V1Role getRoleById(Long id) {
        SysRole role = sysRoleService.getById(id);
        return role != null ? V1Role.from(role) : null;
    }

    @Transactional
    public void createRole(V1RoleRequest request) {
        SysRole role = new SysRole();
        role.setRoleName(request.roleName());
        role.setRoleCode(request.roleCode());
        role.setDescription(request.description());
        sysRoleService.save(role);
    }

    @Transactional
    public void updateRole(Long id, V1RoleRequest request) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(request.roleName());
        role.setRoleCode(request.roleCode());
        role.setDescription(request.description());
        sysRoleService.updateById(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        sysRoleService.removeById(id);
    }

    public List<Long> getMenuIds(Long roleId) {
        return sysRoleService.getMenuIdsByRoleId(roleId);
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        sysRoleService.assignMenus(roleId, menuIds);
    }
}

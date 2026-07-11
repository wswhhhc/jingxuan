package com.jingxuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.entity.SysMenu;
import com.jingxuan.entity.SysRole;
import com.jingxuan.entity.SysRoleMenu;
import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.RoleEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysMenuMapper;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.mapper.SysRoleMenuMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public List<SysMenu> getMenusByRoleId(Long roleId) {
        return sysMenuMapper.selectByRoleId(roleId);
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return sysRoleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        ensureMutableRole(roleId);
        // 物理删除旧关联（UNIQUE KEY 约束下逻辑删除会冲突）
        sysRoleMenuMapper.hardDeleteByRoleId(roleId);

        // 批量插入新关联
        if (menuIds != null && !menuIds.isEmpty()) {
            List<SysRoleMenu> list = menuIds.stream().map(menuId -> {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                return rm;
            }).toList();
            list.forEach(sysRoleMenuMapper::insert);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        ensureMutableRole((Long) id);
        long userCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, ((Long) id).intValue()));
        if (userCount > 0) {
            throw new BusinessException("该角色下存在用户，不允许删除");
        }
        return super.removeById(id);
    }

    @Override
    public boolean updateById(SysRole entity) {
        ensureMutableRole(entity.getId());
        return super.updateById(entity);
    }

    private void ensureMutableRole(Long roleId) {
        if (roleId == null) {
            return;
        }
        SysRole role = getById(roleId);
        if (role != null && RoleEnum.ADMIN.getAuthority().equals(role.getRoleCode())) {
            throw new BusinessException("系统管理员角色不允许在角色权限中修改");
        }
    }
}

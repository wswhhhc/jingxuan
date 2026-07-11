package com.jingxuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.entity.SysMenu;
import com.jingxuan.entity.SysRole;

import java.util.List;

/**
 * 角色服务接口
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 根据角色ID查询菜单列表
     */
    List<SysMenu> getMenusByRoleId(Long roleId);

    /**
     * 获取角色已分配的菜单ID列表
     */
    List<Long> getMenuIdsByRoleId(Long roleId);

    /**
     * 为角色分配菜单
     */
    void assignMenus(Long roleId, List<Long> menuIds);
}

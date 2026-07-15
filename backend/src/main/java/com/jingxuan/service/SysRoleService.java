package com.jingxuan.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.jingxuan.entity.SysRole;

import java.util.List;

/**
 * 角色服务接口
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 获取角色已分配的菜单ID列表
     */
    List<Long> getMenuIdsByRoleId(Long roleId);

    /**
     * 为角色分配菜单
     */
    void assignMenus(Long roleId, List<Long> menuIds);
}


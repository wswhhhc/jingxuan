package com.jingxuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.entity.SysUser;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser findByUsername(String username);

    /**
     * 创建用户
     */
    boolean createUser(SysUser user);

    /**
     * 更新用户状态（启用/禁用）
     */
    boolean updateStatus(Long userId, Integer status);

    /**
     * 更新用户信息（含密码加密）
     */
    boolean updateUser(SysUser user);

    /**
     * 删除用户（逻辑删除）
     */
    boolean deleteUser(Long userId);
}

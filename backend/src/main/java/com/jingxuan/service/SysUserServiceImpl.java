package com.jingxuan.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.RoleEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SysUser findByUsername(String username) {
        return sysUserMapper.findByUsername(username);
    }

    @Override
    public boolean createUser(SysUser user) {
        if (sysUserMapper.countByUsername(user.getUsername()) > 0) {
            throw new BusinessException("用户名已存在");
        }
        // 设置默认密码：123456
        if (user.getPassword() == null) {
            user.setPassword(passwordEncoder.encode("123456"));
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        // 默认首次登录
        if (user.getFirstLogin() == null) {
            user.setFirstLogin(true);
        }
        return save(user);
    }

    @Override
    public boolean updateStatus(Long userId, Integer status) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        ensureMutableUser(user);
        user.setStatus(com.jingxuan.enums.UserStatusEnum.of(status));
        return updateById(user);
    }

    @Override
    public boolean updateUser(SysUser user) {
        SysUser existingUser = getById(user.getId());
        if (existingUser == null) {
            throw new BusinessException("用户不存在");
        }
        ensureMutableUser(existingUser);
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return updateById(user);
    }

    private void ensureMutableUser(SysUser user) {
        boolean protectedAdmin = user.getRoleId() != null
                && user.getRoleId() == RoleEnum.ADMIN.getValue()
                && "admin".equalsIgnoreCase(user.getUsername());
        if (protectedAdmin) {
            throw new BusinessException("系统管理员不允许修改");
        }
    }

    @Override
    public boolean deleteUser(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        ensureMutableUser(user);
        // 物理删除（不再保留软删除记录，释放邮箱和用户名供重新注册）
        return sysUserMapper.physicalDeleteById(userId) > 0;
    }
}

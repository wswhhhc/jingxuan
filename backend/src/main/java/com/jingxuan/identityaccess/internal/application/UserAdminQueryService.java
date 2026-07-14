package com.jingxuan.identityaccess.internal.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.api.V1Page;
import com.jingxuan.api.V1PageInfo;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysRole;
import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.identityaccess.api.V1User;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理查询用例 — 委托给现有 SysUserService。
 */
@Service
@RequiredArgsConstructor
public class UserAdminQueryService {

    private final SysUserService sysUserService;
    private final SysRoleMapper sysRoleMapper;
    private final SysDictMapper sysDictMapper;

    public V1Page<V1User> listUsers(int page, int size, String keyword, Integer roleId, Integer status) {
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

        Map<Long, String> roleMap = sysRoleMapper.selectList(null).stream()
                .collect(Collectors.toMap(r -> ((Number) r.getId()).longValue(), SysRole::getRoleName, (a, b) -> a));
        Map<Long, String> classMap = sysDictMapper.selectList(null).stream()
                .filter(d -> "class".equals(d.getDictType()))
                .collect(Collectors.toMap(d -> ((Number) d.getId()).longValue(), SysDict::getDictLabel, (a, b) -> a));

        List<V1User> items = mpPage.getRecords().stream()
                .map(u -> {
                    String roleName = u.getRoleId() != null ? roleMap.getOrDefault(((Number) u.getRoleId()).longValue(), "") : "";
                    String className = u.getClassId() != null ? classMap.getOrDefault(((Number) u.getClassId()).longValue(), "") : "";
                    return V1User.from(u, roleName, className);
                })
                .toList();

        return new V1Page<>(items, V1PageInfo.of(page, size, mpPage.getTotal()));
    }

    public V1User getUserById(Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return null;
        }
        String roleName = "";
        String className = "";
        if (user.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectById(user.getRoleId());
            if (role != null) {
                roleName = role.getRoleName();
            }
        }
        if (user.getClassId() != null) {
            SysDict dict = sysDictMapper.selectById(user.getClassId());
            if (dict != null) {
                className = dict.getDictLabel();
            }
        }
        return V1User.from(user, roleName, className);
    }
}

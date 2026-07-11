package com.jingxuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.entity.SysMenu;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysMenuMapper;
import com.jingxuan.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private static final Set<String> HIDDEN_MENU_NAMES = Set.of("测试菜单", "t");

    @Override
    public List<SysMenu> getMenuTree() {
        List<SysMenu> allMenus = list().stream()
                .filter(menu -> !HIDDEN_MENU_NAMES.contains(menu.getMenuName()))
                .collect(Collectors.toList());
        return buildTree(allMenus, 0L);
    }

    @Override
    public List<SysMenu> getByParentId(Long parentId) {
        return list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, parentId)
                .orderByAsc(SysMenu::getSort));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        long subCount = count(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (subCount > 0) {
            throw new BusinessException("存在子菜单，不允许删除");
        }
        return super.removeById(id);
    }

    private List<SysMenu> buildTree(List<SysMenu> allMenus, Long parentId) {
        return allMenus.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .peek(m -> m.setChildren(buildTree(allMenus, m.getId())))
                .collect(Collectors.toList());
    }
}

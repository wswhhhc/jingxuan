package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.api.V1Ids;
import com.jingxuan.entity.SysMenu;
import com.jingxuan.enums.MenuTypeEnum;
import com.jingxuan.identityaccess.api.V1Menu;
import com.jingxuan.identityaccess.api.V1MenuRequest;
import com.jingxuan.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 菜单管理命令用例 — 委托给旧 SysMenuService。 */
@Service
@RequiredArgsConstructor
public class MenuAdminCommandService {

    private final SysMenuService sysMenuService;

    public List<V1Menu> getMenuTree() {
        return sysMenuService.getMenuTree().stream()
                .map(V1Menu::from)
                .toList();
    }

    public V1Menu getMenuById(Long id) {
        SysMenu menu = sysMenuService.getById(id);
        return menu != null ? V1Menu.from(menu) : null;
    }

    @Transactional
    public void createMenu(V1MenuRequest request) {
        SysMenu menu = new SysMenu();
        menu.setMenuName(request.menuName());
        if (request.parentId() != null && !request.parentId().isBlank()) {
            menu.setParentId(V1Ids.parse(request.parentId(), "parentId"));
        }
        menu.setPath(request.path());
        menu.setPermission(request.permission());
        if (request.type() != null) {
            menu.setType(MenuTypeEnum.valueOf(request.type()));
        }
        menu.setIcon(request.icon());
        menu.setSort(request.sort());
        sysMenuService.save(menu);
    }

    @Transactional
    public void updateMenu(Long id, V1MenuRequest request) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setMenuName(request.menuName());
        if (request.parentId() != null && !request.parentId().isBlank()) {
            menu.setParentId(V1Ids.parse(request.parentId(), "parentId"));
        }
        menu.setPath(request.path());
        menu.setPermission(request.permission());
        if (request.type() != null) {
            menu.setType(MenuTypeEnum.valueOf(request.type()));
        }
        menu.setIcon(request.icon());
        menu.setSort(request.sort());
        sysMenuService.updateById(menu);
    }
}

package com.jingxuan.identityaccess.api;

import com.jingxuan.entity.SysMenu;

import java.util.List;

/** v1 菜单 DTO。 */
public record V1Menu(
        String id,
        String menuName,
        String parentId,
        String path,
        String permission,
        String type,
        String icon,
        Integer sort,
        List<V1Menu> children
) {
    public static V1Menu from(SysMenu menu) {
        List<V1Menu> childList = menu.getChildren() != null
                ? menu.getChildren().stream().map(V1Menu::from).toList()
                : null;
        return new V1Menu(
                menu.getId() != null ? menu.getId().toString() : null,
                menu.getMenuName(),
                menu.getParentId() != null ? menu.getParentId().toString() : null,
                menu.getPath(),
                menu.getPermission(),
                menu.getType() != null ? menu.getType().name() : null,
                menu.getIcon(),
                menu.getSort(),
                childList
        );
    }
}

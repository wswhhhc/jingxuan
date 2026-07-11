package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import com.jingxuan.enums.MenuTypeEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 系统菜单/权限
 */
@Getter
@Setter
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    @TableField("menu_name")
    private String menuName;

    @TableField("parent_id")
    private Long parentId;

    @TableField("path")
    private String path;

    @TableField("permission")
    private String permission;

    @TableField("type")
    private MenuTypeEnum type;

    @TableField("icon")
    private String icon;

    @TableField("sort")
    private Integer sort;

    /** 子菜单（非数据库字段） */
    @TableField(exist = false)
    private List<SysMenu> children;
}

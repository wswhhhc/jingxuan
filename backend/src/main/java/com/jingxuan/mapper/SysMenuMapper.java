package com.jingxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jingxuan.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单 Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据角色ID查询菜单列表
     */
    @Select("SELECT sm.* FROM sys_menu sm " +
            "JOIN sys_role_menu srm ON sm.id = srm.menu_id " +
            "WHERE srm.role_id = #{roleId} AND sm.deleted = 0 " +
            "ORDER BY sm.sort ASC")
    List<SysMenu> selectByRoleId(@Param("roleId") Long roleId);
}

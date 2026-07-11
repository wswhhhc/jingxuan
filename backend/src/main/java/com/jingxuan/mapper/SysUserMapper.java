package com.jingxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jingxuan.entity.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser findByUsername(@Param("username") String username);

    /**
     * 查询用户权限标识列表
     */
    @Select("SELECT DISTINCT sm.permission FROM sys_user su " +
            "JOIN sys_role_menu srm ON su.role_id = srm.role_id " +
            "JOIN sys_menu sm ON srm.menu_id = sm.id " +
            "WHERE su.id = #{userId} AND sm.permission IS NOT NULL AND sm.permission != '' " +
            "AND su.deleted = 0")
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 检查用户名是否已存在
     */
    @Select("SELECT COUNT(1) FROM sys_user WHERE username = #{username} AND deleted = 0")
    int countByUsername(@Param("username") String username);

    @Select("SELECT COUNT(1) FROM sys_user WHERE email = #{email} AND deleted = 0")
    int countByEmail(@Param("email") String email);

    @Select("SELECT COUNT(1) FROM sys_user WHERE email = #{email} AND role_id = #{roleId} AND deleted = 0")
    int countByEmailAndRole(@Param("email") String email, @Param("roleId") Integer roleId);

    /**
     * 物理删除用户（不触发 @TableLogic 逻辑删除）
     */
    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}

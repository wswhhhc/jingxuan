package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import com.jingxuan.enums.UserStatusEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统用户
 */
@Getter
@Setter
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("real_name")
    private String realName;

    @TableField("role_id")
    private Integer roleId;

    @TableField("class_id")
    private Long classId;

    @TableField("avatar")
    private String avatar;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("status")
    private UserStatusEnum status;

    @TableField("first_login")
    private Boolean firstLogin;
}

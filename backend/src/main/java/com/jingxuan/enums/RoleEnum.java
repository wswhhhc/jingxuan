package com.jingxuan.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统角色枚举
 */
@Getter
@AllArgsConstructor
public enum RoleEnum {

    STUDENT(1, "学生", "ROLE_STUDENT"),
    TEACHER(2, "教师", "ROLE_TEACHER"),
    ADMIN(3, "管理员", "ROLE_ADMIN");

    private final int value;
    private final String label;
    private final String authority;
}

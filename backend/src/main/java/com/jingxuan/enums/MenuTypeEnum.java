package com.jingxuan.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 菜单类型
 */
@Getter
@AllArgsConstructor
public enum MenuTypeEnum {

    DIRECTORY(0, "目录"),
    MENU(1, "菜单"),
    BUTTON(2, "按钮");

    @EnumValue
    private final int value;

    @JsonValue
    private final String label;
}

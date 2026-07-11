package com.jingxuan.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户账号状态
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    @EnumValue
    @JsonValue
    private final int value;

    private final String label;

    public static UserStatusEnum of(int value) {
        for (UserStatusEnum s : values()) {
            if (s.value == value) return s;
        }
        return DISABLED;
    }
}

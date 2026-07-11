package com.jingxuan.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 作品发布状态
 * 职责：仅管理前台展示控制，不表达审核含义
 */
@Getter
@AllArgsConstructor
public enum PublishStatusEnum {

    UNPUBLISHED(0, "未发布"),
    PUBLISHED(1, "已发布"),
    OFFLINE(2, "已下线");

    @EnumValue
    private final int value;

    @JsonValue
    private final String label;

    public static PublishStatusEnum of(int value) {
        for (PublishStatusEnum s : values()) {
            if (s.value == value) return s;
        }
        return UNPUBLISHED;
    }
}

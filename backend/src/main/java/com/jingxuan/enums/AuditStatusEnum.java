package com.jingxuan.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 作品审核状态
 * 职责：仅管理审核流转，不表达发布含义
 */
@Getter
@AllArgsConstructor
public enum AuditStatusEnum {

    DRAFT(0, "草稿"),
    SUBMITTED(1, "已提交"),
    REJECTED(2, "已驳回"),
    APPROVED(3, "已通过");

    @EnumValue
    private final int value;

    @JsonValue
    private final String label;

    public static AuditStatusEnum of(int value) {
        for (AuditStatusEnum s : values()) {
            if (s.value == value) return s;
        }
        return DRAFT;
    }
}

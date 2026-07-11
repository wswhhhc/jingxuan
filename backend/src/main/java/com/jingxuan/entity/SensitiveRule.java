package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sensitive_rule")
public class SensitiveRule extends BaseEntity {

    private String ruleName;
    private String systemPrompt;
    private String enabledCategories;
    private String onRejectAction;
    private Integer status;
}

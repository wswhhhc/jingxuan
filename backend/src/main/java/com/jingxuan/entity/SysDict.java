package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_dict")
public class SysDict extends BaseEntity {

    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer sort;
    private String remark;
}

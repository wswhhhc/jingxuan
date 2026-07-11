package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("tag")
public class Tag extends BaseEntity {

    private String name;
    private String color;
    private String type;
    private Integer sort;
}

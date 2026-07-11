package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("work_like")
public class WorkLike {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long workId;
    private Long userId;
}

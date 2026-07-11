package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("work_comment")
public class WorkComment extends BaseEntity {

    private Long workId;
    private Long userId;
    private String guestName;
    private String content;
    private Long parentId;
}

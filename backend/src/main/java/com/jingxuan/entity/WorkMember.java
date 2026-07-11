package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("work_member")
public class WorkMember extends BaseEntity {

    private Long workId;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String className;
    private Integer isLeader;
}

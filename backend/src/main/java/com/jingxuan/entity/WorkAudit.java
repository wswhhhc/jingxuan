package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("work_audit")
public class WorkAudit extends BaseEntity {

    private Long workId;
    private Long auditorId;
    private Integer result;
    private String reason;
    private LocalDateTime auditTime;
}

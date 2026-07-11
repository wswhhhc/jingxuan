package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("work_score")
public class WorkScore extends BaseEntity {

    private Long workId;
    private Long teacherId;
    private BigDecimal innovation;
    private BigDecimal difficulty;
    private BigDecimal completion;
    private BigDecimal practicality;
    private BigDecimal total;
    private String comment;
    private Long batchId;
}

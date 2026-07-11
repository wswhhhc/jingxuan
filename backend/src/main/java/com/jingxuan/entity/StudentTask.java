package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 学生待办任务
 */
@Getter
@Setter
@TableName("student_task")
@Schema(description = "学生待办任务")
public class StudentTask extends BaseEntity {

    @Schema(description = "学生用户ID")
    private Long userId;

    @Schema(description = "关联评分批次ID")
    private Long batchId;

    @Schema(description = "关联作品ID（提交后回填）")
    private Long workId;

    @Schema(description = "待办标题")
    private String title;

    @Schema(description = "待办要求说明")
    private String content;

    @Schema(description = "状态：0=待处理 1=已完成 2=已驳回 3=已截止")
    private Integer status;

    /** 批次名称（非数据库字段，联表查询填充） */
    @TableField(exist = false)
    @Schema(description = "批次名称")
    private String batchName;

    /** 批次结束时间（非数据库字段，用于前端判断截止） */
    @TableField(exist = false)
    @Schema(description = "批次结束时间")
    private String endTime;
}

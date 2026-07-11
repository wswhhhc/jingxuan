package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 作品删除申请
 */
@Getter
@Setter
@TableName("delete_request")
@Schema(description = "作品删除申请")
public class DeleteRequest extends BaseEntity {

    @Schema(description = "申请删除的作品ID")
    private Long workId;

    @Schema(description = "申请人（学生）ID")
    private Long studentId;

    @Schema(description = "申请原因")
    private String reason;

    @Schema(description = "状态：0=待处理 1=已同意 2=已拒绝")
    private Integer status;

    @Schema(description = "管理员回复（拒绝时填原因）")
    private String adminReply;

    /** 作品名称（非数据库字段，联表查询填充） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    @Schema(description = "作品名称")
    private String workTitle;

    /** 申请人姓名（非数据库字段） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    @Schema(description = "申请人姓名")
    private String studentName;
}

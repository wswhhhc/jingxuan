package com.jingxuan.modules.work.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "作品成员信息")
public class WorkMemberDTO {

    @Schema(description = "成员ID（编辑时传入）")
    private Long id;

    @Schema(description = "学生用户ID（已注册用户）")
    private Long studentId;

    @NotBlank(message = "学生姓名不能为空")
    @Schema(description = "学生姓名")
    private String studentName;

    @NotBlank(message = "学号不能为空")
    @Schema(description = "学号")
    private String studentNo;

    @NotBlank(message = "班级名称不能为空")
    @Schema(description = "班级名称")
    private String className;

    @Schema(description = "是否队长 0=否 1=是")
    private Integer isLeader;

    @Schema(description = "用户头像URL（来自 SysUser）")
    private String avatar;
}

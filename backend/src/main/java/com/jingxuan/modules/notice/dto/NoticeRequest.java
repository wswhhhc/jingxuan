package com.jingxuan.modules.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "公告请求")
public class NoticeRequest {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题不超过200字")
    @Schema(description = "公告标题")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    @Schema(description = "公告内容")
    private String content;

    @Schema(description = "是否置顶 0=否 1=是")
    private Integer topFlag;

    @Schema(description = "状态 0=草稿 1=已发布")
    private Integer status;

    @Schema(description = "通知发送范围：student=仅学生  teacher=仅教师  all=全体")
    private String targetScope;
}

package com.jingxuan.modules.work.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "作品请求（创建/更新共用）")
public class WorkRequest {

    @Size(max = 200, message = "作品名称不超过200字")
    @Schema(description = "作品名称（创建时必填）")
    private String title;

    @Schema(description = "作品简介")
    private String summary;

    @Schema(description = "技术栈，逗号分隔")
    private String techStack;

    @Schema(description = "指导教师")
    private String advisor;

    @Schema(description = "封面图URL")
    private String coverUrl;

    @Schema(description = "上传的mp4演示视频文件地址")
    private String videoUrl;

    @Schema(description = "服务器访问地址")
    private String previewUrl;

    @Schema(description = "运行说明")
    private String runDesc;

    @Schema(description = "团队成员列表（队长在第一位，isLeader=1）")
    private List<WorkMemberDTO> members;

    @Schema(description = "附件ID列表（已上传的附件ID）")
    private List<String> attachmentIds;

    @Schema(description = "评分批次ID（不传则自动匹配当前活跃批次，仅创建时使用）")
    private Long batchId;
}

package com.jingxuan.modules.work.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jingxuan.entity.WorkAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "作品详情VO")
public class WorkDetailVO {

    @Schema(description = "作品ID")
    private Long id;

    @Schema(description = "作品名称")
    private String title;

    @Schema(description = "作品简介")
    private String summary;

    @Schema(description = "技术栈")
    private String techStack;

    @Schema(description = "指导教师")
    private String advisor;

    @Schema(description = "封面图")
    private String coverUrl;

    @Schema(description = "上传的mp4演示视频文件地址")
    private String videoUrl;

    @Schema(description = "服务器访问地址")
    private String previewUrl;

    @Schema(description = "运行说明")
    private String runDesc;

    @Schema(description = "审核状态")
    private Integer status;

    @Schema(description = "审核状态名称")
    private String statusLabel;

    @Schema(description = "提交人ID")
    private Long submitterId;

    @Schema(description = "提交人姓名")
    private String submitterName;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "批次ID")
    private Long batchId;

    @Schema(description = "成员列表")
    private List<WorkMemberDTO> members;

    @Schema(description = "附件列表")
    private List<WorkAttachment> attachments;

    @Schema(description = "发布状态")
    private Integer publishStatus;

    @Schema(description = "发布状态名称")
    private String publishStatusLabel;

    @Schema(description = "是否精选")
    private Integer featured;

    @Schema(description = "平均分（审核通过且有评分时，仅排行榜已公示时显示）")
    private String avgScore;

    @Schema(description = "排名（仅排行榜已公示时显示）")
    private Integer rank;

    @Schema(description = "平均创新性得分（仅排行榜已公示时显示）")
    private String avgInnovation;

    @Schema(description = "平均技术难度得分（仅排行榜已公示时显示）")
    private String avgDifficulty;

    @Schema(description = "平均完成度得分（仅排行榜已公示时显示）")
    private String avgCompletion;

    @Schema(description = "平均实用性得分（仅排行榜已公示时显示）")
    private String avgPracticality;

    @Schema(description = "评分教师数（仅排行榜已公示时显示）")
    private Integer teacherCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "浏览数")
    private Integer viewCount;

    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;

    @Schema(description = "标签列表")
    private java.util.List<String> tags;
}

package com.jingxuan.modules.work.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "作品列表VO")
public class WorkListVO {

    @Schema(description = "作品ID")
    private Long id;

    @Schema(description = "作品名称")
    private String title;

    @Schema(description = "技术栈")
    private String techStack;

    @Schema(description = "作品简介")
    private String summary;

    @Schema(description = "封面图")
    private String coverUrl;

    @Schema(description = "服务器访问地址")
    private String previewUrl;

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

    @Schema(description = "发布状态")
    private Integer publishStatus;

    @Schema(description = "批次ID")
    private Long batchId;

    @Schema(description = "批次名称")
    private String batchName;

    @Schema(description = "是否精选")
    private Integer featured;

    @Schema(description = "成员数")
    private Integer memberCount;

    @Schema(description = "当前教师是否已评分")
    private Boolean scored;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "浏览数")
    private Integer viewCount;

    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;

    @Schema(description = "标签列表")
    private java.util.List<String> tags;
}

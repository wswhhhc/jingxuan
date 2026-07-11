package com.jingxuan.modules.work.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "作品查询请求")
public class WorkQueryRequest {

    @Schema(description = "审核状态")
    private Integer status;

    @Schema(description = "提交人ID（学生查询自己的）")
    private Long submitterId;

    @Schema(description = "参与人ID（提交者或已注册成员）")
    private Long participantUserId;

    @Schema(description = "评分批次ID")
    private Long batchId;

    @Schema(description = "关键词搜索")
    private String keyword;

    @Schema(description = "技术栈筛选")
    private String techStack;

    @Schema(description = "班级ID（通过 sys_user.class_id 筛选）")
    private Long classId;

    @Schema(description = "提交时间起始")
    private String submitTimeBegin;

    @Schema(description = "提交时间截止")
    private String submitTimeEnd;

    @Schema(description = "排除的作品ID列表")
    private List<Long> excludeWorkIds;

    @Schema(description = "页码")
    private int pageNum = 1;

    @Schema(description = "每页大小")
    private int pageSize = 10;
}

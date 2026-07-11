package com.jingxuan.modules.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "审核历史VO")
public class AuditHistoryVO {

    @Schema(description = "审核记录ID")
    private Long id;

    @Schema(description = "作品ID")
    private Long workId;

    @Schema(description = "作品名称")
    private String workTitle;

    @Schema(description = "审核人姓名")
    private String auditorName;

    @Schema(description = "审核结果 0=驳回 1=通过")
    private Integer result;

    @Schema(description = "审核结果名称")
    private String resultLabel;

    @Schema(description = "驳回原因")
    private String reason;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;
}

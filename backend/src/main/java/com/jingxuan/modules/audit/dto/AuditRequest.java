package com.jingxuan.modules.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "审核请求")
public class AuditRequest {

    @NotNull(message = "作品ID不能为空")
    @Schema(description = "作品ID")
    private Long workId;

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果 0=驳回 1=通过")
    private Integer result;

    @Schema(description = "驳回原因（驳回时必填）")
    private String reason;
}

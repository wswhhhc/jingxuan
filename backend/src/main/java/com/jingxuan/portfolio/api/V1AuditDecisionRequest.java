package com.jingxuan.portfolio.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 管理员对已提交作品作出的审核决定。 */
public record V1AuditDecisionRequest(@NotBlank String decision, @Size(max = 500) String reason) {
}

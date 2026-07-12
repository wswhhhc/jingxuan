package com.jingxuan.identityaccess.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 管理员对待审核教师作出的审核决定。 */
public record V1UserApprovalDecisionRequest(
        @Pattern(regexp = "APPROVED|REJECTED", message = "decision 必须为 APPROVED 或 REJECTED") String decision,
        @Size(max = 500, message = "reason 不能超过500个字符") String reason
) {
}

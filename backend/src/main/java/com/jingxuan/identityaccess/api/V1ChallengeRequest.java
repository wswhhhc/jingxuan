package com.jingxuan.identityaccess.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "创建一次性算术 challenge 请求")
public record V1ChallengeRequest(
        @NotNull(message = "challenge 用途不能为空")
        @Schema(description = "challenge 用途", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "LOGIN")
        ChallengePurpose purpose
) {
}

package com.jingxuan.evaluation.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** v1 发放奖品请求。 */
@Schema(description = "发放奖品请求")
public record V1IssueRequest(
        @NotNull @Pattern(regexp = "[0-9]{1,19}") String workId
) {}

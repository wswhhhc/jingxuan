package com.jingxuan.moderation.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "V1ModerationResult", description = "内容审核结果")
public record V1ModerationResult(
    String ruleId,
    String ruleName,
    boolean passed,
    String rejectedCategories,
    String reason
) {}

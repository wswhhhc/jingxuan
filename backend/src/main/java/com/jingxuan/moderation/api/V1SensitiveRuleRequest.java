package com.jingxuan.moderation.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "V1SensitiveRuleRequest", description = "创建/更新敏感词规则请求")
public record V1SensitiveRuleRequest(
    @NotBlank String ruleName,
    @NotBlank String systemPrompt,
    String enabledCategories,
    String onRejectAction
) {}

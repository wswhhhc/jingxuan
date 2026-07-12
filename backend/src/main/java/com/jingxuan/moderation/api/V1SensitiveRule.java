package com.jingxuan.moderation.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(name = "V1SensitiveRule", description = "敏感词/内容审核规则")
public record V1SensitiveRule(
    String id,
    String ruleName,
    String systemPrompt,
    String enabledCategories,
    String onRejectAction,
    boolean enabled,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}

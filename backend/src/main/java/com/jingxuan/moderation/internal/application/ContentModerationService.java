package com.jingxuan.moderation.internal.application;

import com.jingxuan.entity.SensitiveRule;
import com.jingxuan.moderation.api.V1ModerationResult;
import com.jingxuan.modules.sensitive.service.DeepSeekReviewService;
import com.jingxuan.modules.sensitive.service.SensitiveRuleService;
import org.springframework.stereotype.Service;

/** DeepSeek 内容审核应用服务。 */
@Service
public class ContentModerationService {

    private final DeepSeekReviewService deepSeekReviewService;
    private final SensitiveRuleService sensitiveRuleService;

    public ContentModerationService(DeepSeekReviewService deepSeekReviewService,
                                    SensitiveRuleService sensitiveRuleService) {
        this.deepSeekReviewService = deepSeekReviewService;
        this.sensitiveRuleService = sensitiveRuleService;
    }

    /**
     * 按指定规则审核文本内容。
     *
     * @param text   待审核文本
     * @param scene  审核场景（comment/score/profile）
     * @param ruleId 可选的规则 ID，若为空则使用默认系统规则
     */
    public V1ModerationResult review(String text, String scene, Long ruleId) {
        String ruleName;
        if (ruleId != null) {
            SensitiveRule rule = sensitiveRuleService.getById(ruleId);
            ruleName = rule != null ? rule.getRuleName() : "default";
        } else {
            ruleName = "default";
        }

        DeepSeekReviewService.ReviewResult result = deepSeekReviewService.review(text, scene);

        return new V1ModerationResult(
                ruleId != null ? ruleId.toString() : null,
                ruleName,
                result.isPassed(),
                result.isPassed() ? null : result.getCategory(),
                result.isPassed() ? null : result.getReason()
        );
    }
}

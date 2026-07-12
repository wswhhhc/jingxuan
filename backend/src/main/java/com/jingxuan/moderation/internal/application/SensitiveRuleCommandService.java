package com.jingxuan.moderation.internal.application;

import com.jingxuan.entity.SensitiveRule;
import com.jingxuan.moderation.api.V1SensitiveRuleRequest;
import com.jingxuan.modules.sensitive.service.SensitiveRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 敏感词规则命令服务。 */
@Service
public class SensitiveRuleCommandService {

    private final SensitiveRuleService sensitiveRuleService;

    public SensitiveRuleCommandService(SensitiveRuleService sensitiveRuleService) {
        this.sensitiveRuleService = sensitiveRuleService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createRule(V1SensitiveRuleRequest request) {
        var entity = new SensitiveRule();
        entity.setRuleName(request.ruleName());
        entity.setSystemPrompt(request.systemPrompt());
        entity.setEnabledCategories(request.enabledCategories());
        entity.setOnRejectAction(request.onRejectAction());
        return sensitiveRuleService.createRule(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRule(Long id, V1SensitiveRuleRequest request) {
        var entity = new SensitiveRule();
        entity.setId(id);
        entity.setRuleName(request.ruleName());
        entity.setSystemPrompt(request.systemPrompt());
        entity.setEnabledCategories(request.enabledCategories());
        entity.setOnRejectAction(request.onRejectAction());
        sensitiveRuleService.updateRule(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        sensitiveRuleService.toggleStatus(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        sensitiveRuleService.removeById(id);
    }
}

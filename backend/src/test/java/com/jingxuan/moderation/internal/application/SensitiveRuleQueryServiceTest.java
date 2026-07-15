package com.jingxuan.moderation.internal.application;

import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SensitiveRule;
import com.jingxuan.modules.sensitive.service.SensitiveRuleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SensitiveRuleQueryServiceTest {

    private final SensitiveRuleService ruleService = mock(SensitiveRuleService.class);
    private final SensitiveRuleQueryService queryService = new SensitiveRuleQueryService(ruleService);

    @Test
    void queriesRules() {
        SensitiveRule rule = new SensitiveRule(); rule.setId(1L); rule.setRuleName("规则1");
        when(ruleService.queryRuleList(1, 10, null))
                .thenReturn(new PageResult<>(List.of(rule), 1L, 1L, 10L));
        var page = queryService.queryRuleList(1, 10, null);
        assertEquals(1, page.items().size());
    }
}

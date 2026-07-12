package com.jingxuan.moderation.internal.application;

import com.jingxuan.api.V1Page;
import com.jingxuan.api.V1PageInfo;
import com.jingxuan.entity.SensitiveRule;
import com.jingxuan.moderation.api.V1SensitiveRule;
import com.jingxuan.modules.sensitive.service.SensitiveRuleService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 敏感词规则查询服务。 */
@Service
public class SensitiveRuleQueryService {

    private final SensitiveRuleService sensitiveRuleService;

    public SensitiveRuleQueryService(SensitiveRuleService sensitiveRuleService) {
        this.sensitiveRuleService = sensitiveRuleService;
    }

    public V1Page<V1SensitiveRule> queryRuleList(int pageNum, int pageSize, String keyword) {
        var oldPage = sensitiveRuleService.queryRuleList(pageNum, pageSize, keyword);
        var items = oldPage.getRecords().stream()
                .map(this::toDto)
                .toList();
        var pageInfo = V1PageInfo.of(pageNum, pageSize, oldPage.getTotal());
        return new V1Page<>(items, pageInfo);
    }

    public V1SensitiveRule getById(Long id) {
        var entity = sensitiveRuleService.getById(id);
        if (entity == null) return null;
        return toDto(entity);
    }

    private V1SensitiveRule toDto(SensitiveRule entity) {
        return new V1SensitiveRule(
                entity.getId().toString(),
                entity.getRuleName(),
                entity.getSystemPrompt(),
                entity.getEnabledCategories(),
                entity.getOnRejectAction(),
                entity.getStatus() != null && entity.getStatus() == 1,
                toOffset(entity.getCreateTime()),
                toOffset(entity.getUpdateTime())
        );
    }

    private static OffsetDateTime toOffset(java.time.LocalDateTime ldt) {
        return ldt == null ? null : ldt.atOffset(ZoneOffset.ofHours(8));
    }
}

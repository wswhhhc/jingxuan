package com.jingxuan.modules.sensitive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SensitiveRule;

public interface SensitiveRuleService extends IService<SensitiveRule> {

    PageResult<SensitiveRule> queryRuleList(int pageNum, int pageSize, String keyword);

    Long createRule(SensitiveRule rule);

    void updateRule(SensitiveRule rule);

    void toggleStatus(Long id);

    void updateCategories(Long id, String categories);

    void updateFallbackAction(Long id, String action);
}

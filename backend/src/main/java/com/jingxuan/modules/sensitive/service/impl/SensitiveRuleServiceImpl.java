package com.jingxuan.modules.sensitive.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.common.PageResult;
import com.jingxuan.common.PageUtil;
import com.jingxuan.entity.SensitiveRule;
import com.jingxuan.mapper.SensitiveRuleMapper;
import com.jingxuan.modules.sensitive.service.SensitiveRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensitiveRuleServiceImpl extends ServiceImpl<SensitiveRuleMapper, SensitiveRule> implements SensitiveRuleService {

    @Override
    public PageResult<SensitiveRule> queryRuleList(int pageNum, int pageSize, String keyword) {
        return PageUtil.query(pageNum, pageSize, baseMapper,
                w -> w.like(keyword != null && !keyword.isEmpty(), SensitiveRule::getRuleName, keyword)
                      .orderByDesc(SensitiveRule::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRule(SensitiveRule rule) {
        if (rule.getStatus() == null) {
            rule.setStatus(1);
        }
        if (rule.getOnRejectAction() == null) {
            rule.setOnRejectAction("reject");
        }
        baseMapper.insert(rule);
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(SensitiveRule rule) {
        baseMapper.updateById(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        SensitiveRule rule = baseMapper.selectById(id);
        if (rule != null) {
            rule.setStatus(rule.getStatus() != null && rule.getStatus() == 1 ? 0 : 1);
            baseMapper.updateById(rule);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategories(Long id, String categories) {
        SensitiveRule rule = baseMapper.selectById(id);
        if (rule != null) {
            rule.setEnabledCategories(categories);
            baseMapper.updateById(rule);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFallbackAction(Long id, String action) {
        SensitiveRule rule = baseMapper.selectById(id);
        if (rule != null) {
            rule.setOnRejectAction(action);
            baseMapper.updateById(rule);
        }
    }
}

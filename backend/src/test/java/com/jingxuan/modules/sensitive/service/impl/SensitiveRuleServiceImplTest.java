package com.jingxuan.modules.sensitive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SensitiveRule;
import com.jingxuan.mapper.SensitiveRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SensitiveRuleServiceImpl - 敏感规则服务")
class SensitiveRuleServiceImplTest {

    @Mock private SensitiveRuleMapper sensitiveRuleMapper;

    private SensitiveRuleServiceImpl sensitiveRuleService;

    @BeforeEach
    void setUp() {
        sensitiveRuleService = new SensitiveRuleServiceImpl();
        ReflectionTestUtils.setField(sensitiveRuleService, "baseMapper", sensitiveRuleMapper);
    }

    private SensitiveRule createRule(Long id, String name, Integer status) {
        SensitiveRule rule = new SensitiveRule();
        rule.setId(id);
        rule.setRuleName(name);
        rule.setStatus(status);
        rule.setOnRejectAction("reject");
        return rule;
    }

    @Nested
    @DisplayName("规则查询")
    class QueryRule {

        @Test
        @DisplayName("分页查询规则列表（有关键词）")
        void shouldQueryWithKeyword() {
            Page<SensitiveRule> pageResult = new Page<>(1, 10, 1);
            pageResult.setRecords(java.util.List.of(createRule(1L, "测试规则", 1)));
            when(sensitiveRuleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(pageResult);

            PageResult<SensitiveRule> result = sensitiveRuleService.queryRuleList(1, 10, "测试");

            assertEquals(1, result.getTotal());
            assertEquals("测试规则", result.getRecords().get(0).getRuleName());
        }

        @Test
        @DisplayName("分页查询规则列表（无关键词）")
        void shouldQueryWithoutKeyword() {
            Page<SensitiveRule> pageResult = new Page<>(1, 10, 0);
            pageResult.setRecords(java.util.List.of());
            when(sensitiveRuleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(pageResult);

            PageResult<SensitiveRule> result = sensitiveRuleService.queryRuleList(1, 10, null);

            assertEquals(0, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("规则创建")
    class CreateRule {

        @Test
        @DisplayName("创建规则时默认填充 status 和 onRejectAction")
        void shouldSetDefaultsOnCreate() {
            SensitiveRule rule = new SensitiveRule();
            rule.setRuleName("新规则");

            doAnswer(invocation -> {
                SensitiveRule arg = invocation.getArgument(0);
                arg.setId(1L);
                return 1;
            }).when(sensitiveRuleMapper).insert(any(SensitiveRule.class));

            Long id = sensitiveRuleService.createRule(rule);

            assertEquals(Long.valueOf(1L), id);
            assertEquals(Integer.valueOf(1), rule.getStatus());
            assertEquals("reject", rule.getOnRejectAction());
            verify(sensitiveRuleMapper).insert(rule);
        }

        @Test
        @DisplayName("创建规则时保留显式设置的属性")
        void shouldPreserveExplicitValues() {
            SensitiveRule rule = new SensitiveRule();
            rule.setRuleName("宽松规则");
            rule.setStatus(0);
            rule.setOnRejectAction("bypass");

            doAnswer(invocation -> {
                SensitiveRule arg = invocation.getArgument(0);
                arg.setId(2L);
                return 1;
            }).when(sensitiveRuleMapper).insert(any(SensitiveRule.class));

            sensitiveRuleService.createRule(rule);

            assertEquals(Integer.valueOf(0), rule.getStatus());
            assertEquals("bypass", rule.getOnRejectAction());
        }
    }

    @Nested
    @DisplayName("规则更新")
    class UpdateRule {

        @Test
        @DisplayName("更新规则")
        void shouldUpdateRule() {
            SensitiveRule rule = createRule(1L, "更新后规则", 1);

            sensitiveRuleService.updateRule(rule);

            verify(sensitiveRuleMapper).updateById(rule);
        }
    }

    @Nested
    @DisplayName("规则开关")
    class ToggleStatus {

        @Test
        @DisplayName("从启用切换到禁用")
        void shouldToggleFromOnToOff() {
            when(sensitiveRuleMapper.selectById(1L)).thenReturn(createRule(1L, "规则", 1));

            sensitiveRuleService.toggleStatus(1L);

            verify(sensitiveRuleMapper).updateById((SensitiveRule) argThat(rule ->
                    ((SensitiveRule) rule).getStatus() == 0));
        }

        @Test
        @DisplayName("从禁用切换到启用")
        void shouldToggleFromOffToOn() {
            when(sensitiveRuleMapper.selectById(1L)).thenReturn(createRule(1L, "规则", 0));

            sensitiveRuleService.toggleStatus(1L);

            verify(sensitiveRuleMapper).updateById((SensitiveRule) argThat(rule ->
                    ((SensitiveRule) rule).getStatus() == 1));
        }

        @Test
        @DisplayName("规则不存在时静默处理")
        void shouldDoNothingWhenRuleNotFound() {
            when(sensitiveRuleMapper.selectById(999L)).thenReturn(null);

            sensitiveRuleService.toggleStatus(999L);

            verify(sensitiveRuleMapper, never()).updateById(any(SensitiveRule.class));
        }
    }

    @Nested
    @DisplayName("规则分类更新")
    class UpdateCategories {

        @Test
        @DisplayName("更新分类列表")
        void shouldUpdateCategories() {
            when(sensitiveRuleMapper.selectById(1L)).thenReturn(createRule(1L, "规则", 1));

            sensitiveRuleService.updateCategories(1L, "abuse,spam");

            verify(sensitiveRuleMapper).updateById((SensitiveRule) argThat(rule ->
                    "abuse,spam".equals(((SensitiveRule) rule).getEnabledCategories())));
        }
    }

    @Nested
    @DisplayName("回退动作更新")
    class UpdateFallbackAction {

        @Test
        @DisplayName("更新回退动作")
        void shouldUpdateFallbackAction() {
            when(sensitiveRuleMapper.selectById(1L)).thenReturn(createRule(1L, "规则", 1));

            sensitiveRuleService.updateFallbackAction(1L, "warning");

            verify(sensitiveRuleMapper).updateById((SensitiveRule) argThat(rule ->
                    "warning".equals(((SensitiveRule) rule).getOnRejectAction())));
        }
    }
}

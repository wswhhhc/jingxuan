package com.jingxuan.modules.sensitive.controller;

import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.SensitiveRule;
import com.jingxuan.modules.sensitive.service.DeepSeekReviewService;
import com.jingxuan.modules.sensitive.service.SensitiveRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "敏感规则管理", description = "管理端维护内容审核规则")
@RestController
@RequestMapping("/admin/rule")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SensitiveRuleController {

    private final SensitiveRuleService sensitiveRuleService;
    private final DeepSeekReviewService deepSeekReviewService;

    @Operation(summary = "查询规则列表")
    @GetMapping("/list")
    public Result<PageResult<SensitiveRule>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(sensitiveRuleService.queryRuleList(page, size, keyword));
    }

    @Operation(summary = "获取规则详情")
    @GetMapping("/{id}")
    public Result<SensitiveRule> getById(@PathVariable Long id) {
        return Result.ok(sensitiveRuleService.getById(id));
    }

    @Operation(summary = "创建规则")
    @PostMapping
    public Result<Long> create(@RequestBody SensitiveRule rule) {
        return Result.ok(sensitiveRuleService.createRule(rule));
    }

    @Operation(summary = "更新规则")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SensitiveRule rule) {
        rule.setId(id);
        sensitiveRuleService.updateRule(rule);
        return Result.ok();
    }

    @Operation(summary = "删除规则")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sensitiveRuleService.removeById(id);
        return Result.ok();
    }

    @Operation(summary = "启用/禁用规则")
    @PutMapping("/{id}/toggle")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        sensitiveRuleService.toggleStatus(id);
        return Result.ok();
    }

    @Operation(summary = "配置启用的违规类别")
    @PutMapping("/{id}/categories")
    public Result<Void> updateCategories(@PathVariable Long id, @RequestBody Map<String, String> body) {
        sensitiveRuleService.updateCategories(id, body.get("categories"));
        return Result.ok();
    }

    @Operation(summary = "配置兜底策略")
    @PutMapping("/{id}/fallback")
    public Result<Void> updateFallback(@PathVariable Long id, @RequestBody Map<String, String> body) {
        sensitiveRuleService.updateFallbackAction(id, body.get("action"));
        return Result.ok();
    }

    @Operation(summary = "测试 DeepSeek API 连通性")
    @PostMapping("/test-connection")
    public Result<Void> testConnection() {
        return deepSeekReviewService.testConnection();
    }
}

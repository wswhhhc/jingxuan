package com.jingxuan.moderation.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.api.V1Page;
import com.jingxuan.moderation.api.V1SensitiveRule;
import com.jingxuan.moderation.api.V1SensitiveRuleRequest;
import com.jingxuan.moderation.internal.application.SensitiveRuleCommandService;
import com.jingxuan.moderation.internal.application.SensitiveRuleQueryService;
import com.jingxuan.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** v1 敏感词规则管理。 */
@V1Api
@RestController
@RequestMapping("/api/v1/moderation/rules")
@Tag(name = "v1 敏感词规则", description = "敏感词/内容审核规则管理")
public class V1SensitiveRuleController {

    private final SensitiveRuleQueryService queryService;
    private final SensitiveRuleCommandService commandService;

    public V1SensitiveRuleController(SensitiveRuleQueryService queryService,
                                     SensitiveRuleCommandService commandService) {
        this.queryService = queryService;
        this.commandService = commandService;
    }

    @GetMapping
    @Operation(summary = "查询敏感词规则列表")
    public ResponseEntity<V1Page<V1SensitiveRule>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(queryService.queryRuleList(page, size, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取敏感词规则详情")
    public ResponseEntity<V1SensitiveRule> getById(@PathVariable String id) {
        Long ruleId = V1Ids.parse(id, "id");
        V1SensitiveRule rule = queryService.getById(ruleId);
        if (rule == null) {
            throw new NotFoundException("敏感词规则不存在");
        }
        return ResponseEntity.ok(rule);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建敏感词规则")
    public void create(@Valid @RequestBody V1SensitiveRuleRequest request) {
        commandService.createRule(request);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新敏感词规则")
    public void update(@PathVariable String id, @Valid @RequestBody V1SensitiveRuleRequest request) {
        commandService.updateRule(V1Ids.parse(id, "id"), request);
    }

    @PostMapping("/{id}/toggle")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "启用/禁用敏感词规则")
    public void toggle(@PathVariable String id) {
        commandService.toggleStatus(V1Ids.parse(id, "id"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除敏感词规则")
    public void delete(@PathVariable String id) {
        commandService.deleteRule(V1Ids.parse(id, "id"));
    }
}

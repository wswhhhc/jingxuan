package com.jingxuan.referencedata.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.referencedata.api.V1ReferenceItem;
import com.jingxuan.referencedata.api.V1Tag;
import com.jingxuan.referencedata.api.V1TagRequest;
import com.jingxuan.referencedata.api.V1DeletionImpact;
import com.jingxuan.referencedata.internal.application.ReferenceDataCommandService;
import com.jingxuan.referencedata.internal.application.ReferenceDataQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/** v1 班级、字典与标签读取接口。 */
@V1Api
@RestController
@RequestMapping("/api/v1")
@Tag(name = "v1 参考数据", description = "班级、字典和标签")
public class V1ReferenceDataController {

    private final ReferenceDataQueryService queryService;
    private final ReferenceDataCommandService commandService;

    public V1ReferenceDataController(ReferenceDataQueryService queryService, ReferenceDataCommandService commandService) {
        this.queryService = queryService;
        this.commandService = commandService;
    }

    @GetMapping("/classes")
    @Operation(summary = "获取班级")
    public ResponseEntity<List<V1ReferenceItem>> classes() {
        return ResponseEntity.ok(queryService.classes());
    }

    @GetMapping("/dictionaries/{type}")
    @Operation(summary = "按类型获取字典")
    public ResponseEntity<List<V1ReferenceItem>> dictionaries(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String type) {
        return ResponseEntity.ok(queryService.dictionaries(type));
    }

    @GetMapping("/tags")
    @Operation(summary = "获取标签")
    public ResponseEntity<List<V1Tag>> tags(
            @RequestParam(required = false) @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String type) {
        return ResponseEntity.ok(queryService.tags(type));
    }

    @PostMapping("/tags")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建标签")
    public ResponseEntity<V1Tag> createTag(@Valid @RequestBody V1TagRequest request) {
        return ResponseEntity.status(201).body(commandService.createTag(request));
    }

    @PutMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新标签")
    public ResponseEntity<V1Tag> updateTag(@PathVariable Long id, @Valid @RequestBody V1TagRequest request) {
        return ResponseEntity.ok(commandService.updateTag(id, request));
    }

    @GetMapping("/tags/{id}/deletion-impact")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "预览删除标签影响")
    public ResponseEntity<V1DeletionImpact> tagDeletionImpact(@PathVariable Long id) {
        return ResponseEntity.ok(commandService.tagImpact(id));
    }

    @DeleteMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "确认后物理删除标签")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id,
                                          @RequestParam(defaultValue = "false") boolean confirm) {
        commandService.deleteTag(id, confirm);
        return ResponseEntity.noContent().build();
    }
}

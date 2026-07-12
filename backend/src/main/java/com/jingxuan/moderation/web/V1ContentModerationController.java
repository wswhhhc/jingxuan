package com.jingxuan.moderation.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.moderation.api.V1ModerationResult;
import com.jingxuan.moderation.internal.application.ContentModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** v1 内容审核入口。 */
@V1Api
@RestController
@RequestMapping("/api/v1/moderation")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "v1 内容审核", description = "DeepSeek 内容审核")
public class V1ContentModerationController {

    private final ContentModerationService contentModerationService;

    public V1ContentModerationController(ContentModerationService contentModerationService) {
        this.contentModerationService = contentModerationService;
    }

    @PostMapping("/check")
    @Operation(summary = "提交内容审核")
    public ResponseEntity<V1ModerationResult> check(
            @RequestParam(required = false) String ruleId,
            @RequestBody Map<String, @NotBlank String> body) {
        String text = body.get("text");
        String scene = body.getOrDefault("scene", "comment");
        Long rid = ruleId != null ? V1Ids.parse(ruleId, "ruleId") : null;
        V1ModerationResult result = contentModerationService.review(text, scene, rid);
        return ResponseEntity.ok(result);
    }
}

package com.jingxuan.identityaccess.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.identityaccess.api.V1UserApprovalDecisionRequest;
import com.jingxuan.identityaccess.api.V1UserDeletionImpact;
import com.jingxuan.identityaccess.internal.application.UserApprovalService;
import com.jingxuan.identityaccess.internal.application.UserDeletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 管理员审批自助注册教师。 */
@V1Api
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class V1UserApprovalController {

    private final UserApprovalService userApprovalService;
    private final UserDeletionService userDeletionService;

    @PostMapping("/{id}/approval-decisions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('user:approve')")
    @Operation(summary = "审批待审核教师")
    @ApiResponse(responseCode = "204", description = "审批完成")
    public void decide(@PathVariable String id, @Valid @RequestBody V1UserApprovalDecisionRequest request) {
        userApprovalService.decide(V1Ids.parse(id, "id"), request.decision(), request.reason());
    }

    @GetMapping("/{id}/deletion-impact")
    @PreAuthorize("hasAuthority('user:delete')")
    @Operation(summary = "预览删除用户影响")
    @ApiResponse(responseCode = "200", description = "影响清单")
    public V1UserDeletionImpact deletionImpact(@PathVariable String id) {
        return userDeletionService.impact(V1Ids.parse(id, "id"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('user:delete')")
    @Operation(summary = "确认后物理删除用户")
    @ApiResponse(responseCode = "204", description = "用户已物理删除")
    public void delete(@PathVariable String id, @RequestParam(defaultValue = "false") boolean confirm) {
        userDeletionService.delete(V1Ids.parse(id, "id"), confirm);
    }
}

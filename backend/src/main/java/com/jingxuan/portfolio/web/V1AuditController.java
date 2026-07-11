package com.jingxuan.portfolio.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.modules.audit.dto.AuditRequest;
import com.jingxuan.modules.audit.service.AuditService;
import com.jingxuan.portfolio.api.V1AuditDecisionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@V1Api
@RestController
@RequestMapping("/api/v1/works")
@PreAuthorize("hasRole('ADMIN')")
public class V1AuditController {
    private final AuditService auditService;
    public V1AuditController(AuditService auditService) { this.auditService = auditService; }
    @PostMapping("/{id}/audit-decisions")
    public ResponseEntity<Void> decide(@PathVariable String id, @Valid @RequestBody V1AuditDecisionRequest body) {
        AuditRequest request = new AuditRequest(); request.setWorkId(V1Ids.parse(id, "id")); request.setReason(body.reason());
        if ("APPROVED".equals(body.decision())) { request.setResult(1); auditService.approve(request); }
        else if ("REJECTED".equals(body.decision())) { request.setResult(0); auditService.reject(request); }
        else throw new BusinessException(422, "decision 必须为 APPROVED 或 REJECTED");
        return ResponseEntity.noContent().build();
    }
}

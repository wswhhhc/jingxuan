package com.jingxuan.evaluation.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.modules.score.dto.ScoreSubmitRequest;
import com.jingxuan.modules.score.service.ScoreService;
import com.jingxuan.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@V1Api @RestController @RequestMapping("/api/v1/works") @PreAuthorize("hasRole('TEACHER')")
public class V1ScoreController {
 private final ScoreService scoreService;
 public V1ScoreController(ScoreService scoreService) { this.scoreService=scoreService; }
 @PutMapping("/{id}/scores/me") public ResponseEntity<Void> score(@PathVariable String id,@Valid @RequestBody ScoreSubmitRequest request) {
  request.setWorkId(V1Ids.parse(id,"id")); scoreService.submitScore(SecurityUtils.requireCurrentUserId(),request); return ResponseEntity.noContent().build();
 }
}

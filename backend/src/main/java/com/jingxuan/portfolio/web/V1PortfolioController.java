package com.jingxuan.portfolio.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.portfolio.api.V1WorkSummary;
import com.jingxuan.portfolio.api.V1CreateWorkRequest;
import com.jingxuan.portfolio.api.V1CreatedWork;
import com.jingxuan.modules.work.service.WorkService;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.portfolio.api.V1WorkDetail;
import com.jingxuan.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import java.util.List;

@V1Api @RestController @RequestMapping("/api/v1/me")
public class V1PortfolioController {
 private final WorkService workService;
 public V1PortfolioController(WorkService workService) { this.workService=workService; }
 @GetMapping("/works") public List<V1WorkSummary> myWorks() { return workService.getMyWorks(SecurityUtils.requireCurrentUserId()).stream().map(V1WorkSummary::from).toList(); }
 @PostMapping("/works") public ResponseEntity<V1CreatedWork> createWork(@Valid @RequestBody V1CreateWorkRequest request) {
  return ResponseEntity.status(HttpStatus.CREATED).body(V1CreatedWork.draft(workService.createWork(request.toLegacyRequest())));
 }
 @GetMapping("/works/{id}") public V1WorkDetail myWork(@PathVariable String id) { return V1WorkDetail.from(workService.getCurrentStudentWorkDetail(V1Ids.parse(id, "id"))); }
}

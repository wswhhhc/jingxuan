package com.jingxuan.portfolio.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.portfolio.api.V1WorkSummary;
import com.jingxuan.portfolio.api.V1CreateWorkRequest;
import com.jingxuan.portfolio.api.V1CreatedWork;
import com.jingxuan.portfolio.api.V1UpdateWorkRequest;
import com.jingxuan.portfolio.api.V1DeleteRequest;
import com.jingxuan.portfolio.api.V1CreatedDeletionRequest;
import com.jingxuan.modules.deleterequest.service.DeleteRequestService;
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
 private final DeleteRequestService deleteRequestService;
 public V1PortfolioController(WorkService workService, DeleteRequestService deleteRequestService) { this.workService=workService; this.deleteRequestService=deleteRequestService; }
 @GetMapping("/works") public List<V1WorkSummary> myWorks() { return workService.getMyWorks(SecurityUtils.requireCurrentUserId()).stream().map(V1WorkSummary::from).toList(); }
 @PostMapping("/works") public ResponseEntity<V1CreatedWork> createWork(@Valid @RequestBody V1CreateWorkRequest request) {
  return ResponseEntity.status(HttpStatus.CREATED).body(V1CreatedWork.draft(workService.createWork(request.toLegacyRequest())));
 }
 @GetMapping("/works/{id}") public V1WorkDetail myWork(@PathVariable String id) { return V1WorkDetail.from(workService.getCurrentStudentWorkDetail(V1Ids.parse(id, "id"))); }
 @PutMapping("/works/{id}") public ResponseEntity<Void> updateWork(@PathVariable String id, @Valid @RequestBody V1UpdateWorkRequest request) {
  workService.updateWork(V1Ids.parse(id, "id"), request.toLegacyRequest()); return ResponseEntity.noContent().build();
 }
 @PostMapping("/works/{id}/submissions") public ResponseEntity<Void> submitWork(@PathVariable String id) {
  workService.submitWork(V1Ids.parse(id, "id")); return ResponseEntity.noContent().build();
 }
 @PostMapping("/works/{id}/deletion-requests") public ResponseEntity<V1CreatedDeletionRequest> requestDeletion(@PathVariable String id, @Valid @RequestBody V1DeleteRequest request) {
  Long created = deleteRequestService.submitRequest(V1Ids.parse(id, "id"), SecurityUtils.requireCurrentUserId(), request.reason());
  return ResponseEntity.status(HttpStatus.CREATED).body(V1CreatedDeletionRequest.pending(created));
 }
}

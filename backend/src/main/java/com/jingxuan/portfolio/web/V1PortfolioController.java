package com.jingxuan.portfolio.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.portfolio.api.V1WorkSummary;
import com.jingxuan.portfolio.api.V1CreateWorkRequest;
import com.jingxuan.portfolio.api.V1CreatedWork;
import com.jingxuan.portfolio.api.V1UpdateWorkRequest;
import com.jingxuan.portfolio.api.V1DeleteRequest;
import com.jingxuan.portfolio.api.V1CreatedDeletionRequest;
import com.jingxuan.portfolio.api.V1WorkMember;
import com.jingxuan.portfolio.api.V1WorkAttachment;
import com.jingxuan.modules.deleterequest.service.DeleteRequestService;
import com.jingxuan.modules.work.service.WorkService;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkListVO;
import com.jingxuan.portfolio.api.V1WorkDetail;
import com.jingxuan.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import java.time.ZoneOffset;
import java.util.List;

@V1Api @RestController @RequestMapping("/api/v1/me")
public class V1PortfolioController {
 private final WorkService workService;
 private final DeleteRequestService deleteRequestService;
 public V1PortfolioController(WorkService workService, DeleteRequestService deleteRequestService) { this.workService=workService; this.deleteRequestService=deleteRequestService; }
 @GetMapping("/works") public List<V1WorkSummary> myWorks() {
  return workService.getMyWorks(SecurityUtils.requireCurrentUserId()).stream().map(this::toV1WorkSummary).toList();
 }
 @PostMapping("/works") public ResponseEntity<V1CreatedWork> createWork(@Valid @RequestBody V1CreateWorkRequest request) {
  return ResponseEntity.status(HttpStatus.CREATED).body(V1CreatedWork.draft(workService.createWork(request.toLegacyRequest())));
 }
 @GetMapping("/works/{id}") public V1WorkDetail myWork(@PathVariable String id) { return toV1WorkDetail(workService.getCurrentStudentWorkDetail(V1Ids.parse(id, "id"))); }
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

 private V1WorkSummary toV1WorkSummary(WorkListVO v) {
  return new V1WorkSummary(
   idStr(v.getId()), v.getTitle(), v.getSummary(),
   switch (v.getStatus() == null ? 0 : v.getStatus()) { case 1 -> "SUBMITTED"; case 2 -> "REJECTED"; case 3 -> "APPROVED"; default -> "DRAFT"; },
   idStr(v.getSubmitterId()), v.getSubmitterName(), idStr(v.getBatchId()),
   v.getSubmitTime() == null ? null : v.getSubmitTime().atOffset(ZoneOffset.ofHours(8)).toString(), v.getTags() == null ? List.of() : v.getTags());
 }
 private V1WorkDetail toV1WorkDetail(WorkDetailVO v) {
  var members = v.getMembers() == null ? List.<V1WorkMember>of() : v.getMembers().stream().map(m -> new V1WorkMember(
   idStr(m.getId()), idStr(m.getStudentId()), m.getStudentName(), m.getStudentNo(), m.getClassName(),
   Integer.valueOf(1).equals(m.getIsLeader()), m.getAvatar())).toList();
  var atts = v.getAttachments() == null ? List.<V1WorkAttachment>of() : v.getAttachments().stream().map(a ->
   new V1WorkAttachment(idStr(a.getId()), a.getFileName(), a.getFileType(), a.getFileSize(), a.getFileUrl(), a.getCategory())).toList();
  return new V1WorkDetail(idStr(v.getId()), v.getTitle(), v.getSummary(), v.getTechStack(), v.getAdvisor(),
   v.getCoverUrl(), v.getVideoUrl(), v.getPreviewUrl(), v.getRunDesc(),
   switch (v.getStatus() == null ? 0 : v.getStatus()) { case 1 -> "SUBMITTED"; case 2 -> "REJECTED"; case 3 -> "APPROVED"; default -> "DRAFT"; },
   idStr(v.getSubmitterId()), v.getSubmitterName(),
   v.getSubmitTime() == null ? null : v.getSubmitTime().atOffset(ZoneOffset.ofHours(8)).toString(),
   idStr(v.getBatchId()), members, atts,
   switch (v.getPublishStatus() == null ? 0 : v.getPublishStatus()) { case 1 -> "PUBLISHED"; case 2 -> "OFFLINE"; default -> "UNPUBLISHED"; },
   Integer.valueOf(1).equals(v.getFeatured()), v.getAvgScore(), v.getRank(), v.getLikeCount(), v.getViewCount(), v.getLiked(), v.getTags());
 }
 private static String idStr(Long v) { return v == null ? null : v.toString(); }
}

package com.jingxuan.portfolio.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.api.V1Page;
import com.jingxuan.api.V1PageInfo;
import com.jingxuan.modules.adapter.PublicWorkFacade;
import com.jingxuan.modules.adapter.TeacherWorkFacade;
import com.jingxuan.modules.work.service.WorkService;
import com.jingxuan.portfolio.api.V1WorkDetail;
import com.jingxuan.portfolio.api.V1WorkSummary;
import com.jingxuan.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@V1Api @RestController @RequestMapping("/api/v1/showcase")
public class V1ShowcaseController {
 private final WorkService workService;
 private final TeacherWorkFacade teacherWorkFacade;
 private final PublicWorkFacade publicWorkFacade;

 public V1ShowcaseController(WorkService workService, TeacherWorkFacade teacherWorkFacade, PublicWorkFacade publicWorkFacade) {
  this.workService = workService;
  this.teacherWorkFacade = teacherWorkFacade;
  this.publicWorkFacade = publicWorkFacade;
 }

 @GetMapping("/works/{id}") public V1WorkDetail work(@PathVariable String id) { return V1WorkDetail.publicFrom(workService.getPublishedWorkDetail(V1Ids.parse(id, "id"))); }

 /** 作品分页列表：已登录教师查看已审核可评作品，匿名/学生查看已发布作品。 */
 @GetMapping("/works")
 public V1Page<V1WorkSummary> works(
         @RequestParam(defaultValue = "1") int page,
         @RequestParam(defaultValue = "10") int size,
         @RequestParam(required = false) String keyword,
         @RequestParam(required = false) String techStack,
         @RequestParam(required = false) Long classId,
         @RequestParam(required = false) Long batchId,
         @RequestParam(required = false) Boolean onlyUnscored) {
  if (SecurityUtils.isAuthenticated() && SecurityUtils.hasRole("TEACHER")) {
   Long teacherId = SecurityUtils.requireCurrentUserId();
   var result = teacherWorkFacade.queryScoredWorks(page, size, keyword, techStack, batchId, onlyUnscored, teacherId);
   List<V1WorkSummary> items = result.getRecords().stream().map(V1WorkSummary::from).toList();
   return new V1Page<>(items, V1PageInfo.of(page, size, result.getTotal()));
  }
  var result = publicWorkFacade.getPublishedWorks(page, size, keyword, techStack, classId, null, null, null);
  List<V1WorkSummary> items = result.getRecords().stream().map(V1WorkSummary::from).toList();
  return new V1Page<>(items, V1PageInfo.of(page, size, result.getTotal()));
 }
}

package com.jingxuan.portfolio.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.portfolio.api.V1WorkDetail;
import com.jingxuan.modules.work.service.WorkService;
import org.springframework.web.bind.annotation.*;

@V1Api @RestController @RequestMapping("/api/v1/showcase")
public class V1ShowcaseController {
 private final WorkService workService;
 public V1ShowcaseController(WorkService workService) { this.workService=workService; }
 @GetMapping("/works/{id}") public V1WorkDetail work(@PathVariable String id) { return V1WorkDetail.publicFrom(workService.getPublishedWorkDetail(V1Ids.parse(id, "id"))); }
}

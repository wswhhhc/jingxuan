package com.jingxuan.portfolio.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.service.WorkService;
import org.springframework.web.bind.annotation.*;

@V1Api @RestController @RequestMapping("/api/v1/showcase")
public class V1ShowcaseController {
 private final WorkService workService;
 public V1ShowcaseController(WorkService workService) { this.workService=workService; }
 @GetMapping("/works/{id}") public WorkDetailVO work(@PathVariable Long id) { return workService.getApprovedWorkDetail(id); }
}

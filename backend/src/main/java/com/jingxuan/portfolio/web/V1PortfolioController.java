package com.jingxuan.portfolio.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.portfolio.api.V1WorkSummary;
import com.jingxuan.modules.work.service.WorkService;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.portfolio.api.V1WorkDetail;
import com.jingxuan.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@V1Api @RestController @RequestMapping("/api/v1/me")
public class V1PortfolioController {
 private final WorkService workService;
 public V1PortfolioController(WorkService workService) { this.workService=workService; }
 @GetMapping("/works") public List<V1WorkSummary> myWorks() { return workService.getMyWorks(SecurityUtils.requireCurrentUserId()).stream().map(V1WorkSummary::from).toList(); }
 @GetMapping("/works/{id}") public V1WorkDetail myWork(@PathVariable Long id) { return V1WorkDetail.from(workService.getCurrentStudentWorkDetail(id)); }
}

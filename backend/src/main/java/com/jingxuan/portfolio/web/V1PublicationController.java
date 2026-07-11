package com.jingxuan.portfolio.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.modules.publish.service.PublishService;
import com.jingxuan.modules.publish.dto.FeaturedRequest;
import com.jingxuan.portfolio.api.V1FeaturedRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

/** 管理员发布控制命令。 */
@V1Api
@RestController
@RequestMapping("/api/v1/works")
@PreAuthorize("hasRole('ADMIN')")
public class V1PublicationController {
    private final PublishService publishService;
    public V1PublicationController(PublishService publishService) { this.publishService = publishService; }
    @PostMapping("/{id}/publication")
    public ResponseEntity<Void> publish(@PathVariable String id) {
        publishService.publishWork(V1Ids.parse(id, "id")); return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/publication/offline")
    public ResponseEntity<Void> offline(@PathVariable String id) {
        publishService.offlineWork(V1Ids.parse(id, "id")); return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/publication/featured")
    public ResponseEntity<Void> setFeatured(@PathVariable String id, @Valid @RequestBody V1FeaturedRequest body) {
        FeaturedRequest request = new FeaturedRequest(); request.setWorkId(V1Ids.parse(id, "id"));
        request.setFeatured(body.featured() ? 1 : 0); request.setPreviewUrl(body.previewUrl());
        publishService.setFeatured(request); return ResponseEntity.noContent().build();
    }
}

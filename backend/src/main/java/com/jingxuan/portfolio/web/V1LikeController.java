package com.jingxuan.portfolio.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.modules.adapter.PublicWorkFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前过渡期的 v1 幂等点赞入口。 */
@V1Api
@RestController
@RequestMapping("/api/v1/works")
public class V1LikeController {
    private final PublicWorkFacade workFacade;
    public V1LikeController(PublicWorkFacade workFacade) { this.workFacade = workFacade; }
    @PutMapping("/{id}/likes")
    public ResponseEntity<Void> like(@PathVariable String id) { workFacade.ensureLiked(V1Ids.parse(id, "id")); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/{id}/likes")
    public ResponseEntity<Void> unlike(@PathVariable String id) { workFacade.ensureUnliked(V1Ids.parse(id, "id")); return ResponseEntity.noContent().build(); }
}

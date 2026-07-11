package com.jingxuan.portfolio.web;
import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Ids;
import com.jingxuan.modules.comment.service.CommentService;
import com.jingxuan.portfolio.api.V1CreateCommentRequest;
import com.jingxuan.portfolio.api.V1CreatedComment;
import com.jingxuan.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@V1Api @RestController @RequestMapping("/api/v1/works")
public class V1CommentController {
 private final CommentService commentService;
 public V1CommentController(CommentService commentService) { this.commentService=commentService; }
 @PostMapping("/{id}/comments") public ResponseEntity<V1CreatedComment> create(@PathVariable String id,@Valid @RequestBody V1CreateCommentRequest body) {
  Long created=commentService.addComment(V1Ids.parse(id,"id"),SecurityUtils.getCurrentUserId(),body.content(),body.parentId()==null?null:V1Ids.parse(body.parentId(),"parentId"),body.guestName());
  return ResponseEntity.status(201).body(new V1CreatedComment(created.toString()));
 }
}

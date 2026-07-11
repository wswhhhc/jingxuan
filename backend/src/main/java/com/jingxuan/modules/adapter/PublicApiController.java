package com.jingxuan.modules.adapter;

import com.jingxuan.common.PageResult;
import com.jingxuan.common.Result;
import com.jingxuan.entity.SysDict;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 前台展示 API 适配：仅保留路由和参数绑定，业务组装交给 facade。
 */
@Tag(name = "前台展示API适配")
@RestController
@RequiredArgsConstructor
public class PublicApiController {

    private final PublicWorkFacade publicWorkFacade;

    @Operation(summary = "获取已发布作品列表")
    @GetMapping("/public/works")
    public Result<PageResult<WorkListVO>> getPublishedWorks(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String techStack,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime submitTimeBegin,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime submitTimeEnd) {
        return Result.ok(publicWorkFacade.getPublishedWorks(page, size, keyword, techStack, classId,
                tagIds, submitTimeBegin, submitTimeEnd));
    }

    @Operation(summary = "获取班级列表（公开）")
    @GetMapping("/public/classes")
    public Result<List<SysDict>> getPublicClassList() {
        return Result.ok(publicWorkFacade.getPublicClasses());
    }

    @Operation(summary = "获取标签列表（公开）")
    @GetMapping("/public/tags")
    public Result<List<com.jingxuan.entity.SysDict>> getPublicTagList() {
        return Result.ok(publicWorkFacade.getPublicTags());
    }

    @Operation(summary = "获取已发布作品详情")
    @GetMapping("/public/works/{id}")
    public Result<WorkDetailVO> getPublishedWorkDetail(@PathVariable Long id) {
        return Result.ok(publicWorkFacade.getPublishedWorkDetail(id));
    }

    @Operation(summary = "点赞/取消点赞作品")
    @PostMapping("/works/{id}/like")
    public Result<Map<String, Object>> toggleLike(@PathVariable Long id) {
        return Result.ok(publicWorkFacade.toggleLike(id));
    }

    @Operation(summary = "获取作品点赞状态（未登录时 liked=false）")
    @GetMapping("/public/works/{id}/like-status")
    public Result<Map<String, Object>> getLikeStatus(@PathVariable Long id) {
        return Result.ok(publicWorkFacade.getLikeStatus(id));
    }
}

package com.jingxuan.communication.web;

import com.jingxuan.api.V1Api;
import com.jingxuan.api.V1Page;
import com.jingxuan.communication.api.V1Notice;
import com.jingxuan.communication.api.V1NoticeRequest;
import com.jingxuan.communication.internal.application.NoticeCommandService;
import com.jingxuan.communication.internal.application.NoticeQueryService;
import com.jingxuan.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@V1Api
@RestController
@RequestMapping("/api/v1/notices")
@Tag(name = "公告管理")
public class V1NoticeController {

    private final NoticeQueryService noticeQueryService;
    private final NoticeCommandService noticeCommandService;

    public V1NoticeController(NoticeQueryService noticeQueryService, NoticeCommandService noticeCommandService) {
        this.noticeQueryService = noticeQueryService;
        this.noticeCommandService = noticeCommandService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员查询公告列表（支持按状态筛选）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "公告列表"),
        @ApiResponse(responseCode = "403", description = "非管理员禁止访问")
    })
    public V1Page<V1Notice> listNotices(
            @RequestParam(defaultValue = "1") @jakarta.validation.constraints.Min(1) int page,
            @RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int pageSize,
            @RequestParam(required = false) Integer status) {
        return noticeQueryService.listNotices(page, pageSize, status);
    }

    @GetMapping("/published")
    @Operation(summary = "查询已发布公告（公开端）")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "已发布公告列表"))
    public V1Page<V1Notice> listPublishedNotices(
            @RequestParam(defaultValue = "1") @jakarta.validation.constraints.Min(1) int page,
            @RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int pageSize) {
        return noticeQueryService.listPublishedNotices(page, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取公告详情（公开端）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "公告详情"),
        @ApiResponse(responseCode = "404", description = "公告不存在")
    })
    public V1Notice getNotice(@PathVariable String id) {
        return noticeQueryService.getNotice(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建公告")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "创建成功"),
        @ApiResponse(responseCode = "403", description = "非管理员禁止访问"),
        @ApiResponse(responseCode = "422", description = "参数校验失败")
    })
    public V1Notice createNotice(@Valid @RequestBody V1NoticeRequest request) {
        String id = noticeCommandService.createNotice(request, SecurityUtils.requireCurrentUserId());
        return noticeQueryService.getNotice(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新公告")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "403", description = "非管理员禁止访问"),
        @ApiResponse(responseCode = "404", description = "公告不存在"),
        @ApiResponse(responseCode = "422", description = "参数校验失败")
    })
    public V1Notice updateNotice(@PathVariable String id, @Valid @RequestBody V1NoticeRequest request) {
        noticeCommandService.updateNotice(id, request);
        return noticeQueryService.getNotice(id);
    }

    @PostMapping("/{id}/publication")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "发布公告")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "发布成功"),
        @ApiResponse(responseCode = "403", description = "非管理员禁止访问"),
        @ApiResponse(responseCode = "404", description = "公告不存在")
    })
    public void publishNotice(@PathVariable String id) {
        noticeCommandService.publishNotice(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除公告")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "403", description = "非管理员禁止访问"),
        @ApiResponse(responseCode = "404", description = "公告不存在")
    })
    public void deleteNotice(@PathVariable String id) {
        noticeCommandService.deleteNotice(id);
    }
}

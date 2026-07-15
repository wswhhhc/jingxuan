package com.jingxuan.modules.work.controller;

import com.jingxuan.common.Result;
import com.jingxuan.constant.CommonConstants;
import com.jingxuan.entity.WorkAttachment;
import com.jingxuan.entity.Work;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.WorkAttachmentMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.security.SecurityUtils;
import com.jingxuan.util.FileUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/file")
@Tag(name = "文件上传")
public class FileUploadController {

    @Value("${jingxuan.upload.path:./uploads}")
    private String uploadDir;

    @Autowired
    private WorkAttachmentMapper workAttachmentMapper;

    @Autowired
    private WorkMapper workMapper;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            log.warn("无法创建上传目录: {}", uploadDir);
        }
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传作品附件（压缩包、图片、视频等）")
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                  @RequestParam(value = "workId", required = false) Long workId) {
        if (file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            return Result.fail("文件名无效");
        }

        // 校验扩展名
        FileUtil.validateExtension(originalName);
        String ext = FileUtil.getExtension(originalName);

        // 校验 MIME 类型（基于客户端声明，仅做辅助校验）
        String mimeType = file.getContentType();
        if (mimeType != null && !mimeType.isEmpty() && !FileUtil.isAllowedMimeType(mimeType)) {
            return Result.fail("不支持的文件类型: " + mimeType);
        }

        // 基于文件魔数检测真实类型，防止扩展名伪造
        // getInputStream() 可重复调用，不影响后续 file.transferTo()
        try (java.io.InputStream in = file.getInputStream()) {
            String realType = FileUtil.detectRealType(in);
            if (FileUtil.isRealTypeMismatch(realType, ext)) {
                log.warn("文件类型不匹配: 声明为 .{}, 实际为 {}, fileName={}", ext, realType, originalName);
                return Result.fail("文件内容与扩展名不匹配，已拦截");
            }
        } catch (IOException e) {
            log.warn("文件类型检测流读取异常: {}", e.getMessage());
            // 检测失败时仅依赖扩展名校验，不拦截上传
        }

        // 校验文件大小
        long size = file.getSize();
        if (isImage(ext) && size > CommonConstants.IMAGE_MAX_SIZE) {
            return Result.fail("图片文件不能超过10MB");
        }
        if (isVideo(ext)) {
            if (size > CommonConstants.VIDEO_MAX_SIZE) {
                return Result.fail("视频文件不能超过1.5GB");
            }
        } else if (size > CommonConstants.FILE_MAX_SIZE) {
            return Result.fail("压缩包文件不能超过200MB");
        }

        try {
            // 生成唯一文件名
            String newName = FileUtil.generateFileName(originalName);
            String dateDir = new java.text.SimpleDateFormat("yyyy/MM/dd").format(new java.util.Date());
            String relativePath = dateDir + "/" + newName;

            Path targetPath = Paths.get(uploadDir, relativePath);
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);

            String url = "/uploads/" + relativePath.replace("\\", "/");

            // 入库附件记录，以便后续与作品关联
            WorkAttachment attachment = new WorkAttachment();
            attachment.setFileName(originalName);
            attachment.setFileType(ext);
            attachment.setFileSize(size);
            attachment.setFileUrl(url);
            if (workId != null) {
                Work work = workMapper.selectById(workId);
                if (work == null) {
                    throw new BusinessException("作品不存在，无法绑定附件");
                }
                Long currentUserId = SecurityUtils.requireCurrentUserId();
                if (!currentUserId.equals(work.getSubmitterId())) {
                    throw new BusinessException("无权向该作品上传附件");
                }
                attachment.setWorkId(workId);
            }
            workAttachmentMapper.insert(attachment);

            // 注意：Snowflake ID（19位）超出 JS Number.MAX_SAFE_INTEGER，
            // 必须用 String.valueOf 转成字符串返回，否则前端 JS 会丢失精度
            Map<String, Object> result = new HashMap<>();
            result.put("id", String.valueOf(attachment.getId()));
            result.put("url", url);
            result.put("originalName", originalName);
            result.put("fileSize", String.valueOf(size));
            result.put("fileType", ext);

            return Result.ok(result);
        } catch (IOException e) {
            log.error("文件上传失败: {}", originalName, e);
            return Result.fail("文件上传失败: " + e.getMessage());
        }
    }

    private boolean isImage(String ext) {
        return "jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext) || "gif".equals(ext);
    }

    private boolean isVideo(String ext) {
        return "mp4".equals(ext);
    }
}

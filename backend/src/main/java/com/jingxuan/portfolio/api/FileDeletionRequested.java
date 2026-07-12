package com.jingxuan.portfolio.api;

import java.util.Optional;

/** 已提交的业务事务要求删除一个本地上传文件。 */
public record FileDeletionRequested(String relativePath) {

    private static final String UPLOADS_PREFIX = "/uploads/";

    public static Optional<FileDeletionRequested> fromUploadUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(UPLOADS_PREFIX)) {
            return Optional.empty();
        }
        String path = fileUrl.substring(UPLOADS_PREFIX.length());
        if (path.isBlank() || path.startsWith("/") || path.contains("..") || path.contains("\\")) {
            return Optional.empty();
        }
        return Optional.of(new FileDeletionRequested(path));
    }
}

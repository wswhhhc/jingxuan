package com.jingxuan.portfolio.api;

import com.jingxuan.entity.WorkAttachment;

/** v1 作品附件元数据。 */
public record V1WorkAttachment(String id, String fileName, String contentType, Long size,
                               String url, String category) {
    public static V1WorkAttachment from(WorkAttachment value) {
        return new V1WorkAttachment(id(value.getId()), value.getFileName(), value.getFileType(), value.getFileSize(),
                value.getFileUrl(), value.getCategory());
    }

    private static String id(Long value) { return value == null ? null : value.toString(); }
}

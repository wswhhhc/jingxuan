package com.jingxuan.api;

/** v1 列表资源统一分页元数据。 */
public record V1PageInfo(int page, int pageSize, long total, long totalPages) {

    public static V1PageInfo of(int page, int pageSize, long total) {
        if (page < 1 || pageSize < 1 || pageSize > 100 || total < 0) {
            throw new IllegalArgumentException("分页参数无效");
        }
        return new V1PageInfo(page, pageSize, total, (total + pageSize - 1) / pageSize);
    }
}

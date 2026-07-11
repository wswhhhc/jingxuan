package com.jingxuan.portfolio.api;

/** 删除申请创建结果。 */
public record V1CreatedDeletionRequest(String id, String status) {
    public static V1CreatedDeletionRequest pending(Long id) { return new V1CreatedDeletionRequest(id.toString(), "PENDING"); }
}

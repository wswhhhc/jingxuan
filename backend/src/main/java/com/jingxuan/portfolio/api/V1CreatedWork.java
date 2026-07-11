package com.jingxuan.portfolio.api;

/** 新建草稿的命令结果。 */
public record V1CreatedWork(String id, String status) {
    public static V1CreatedWork draft(Long id) { return new V1CreatedWork(id.toString(), "DRAFT"); }
}

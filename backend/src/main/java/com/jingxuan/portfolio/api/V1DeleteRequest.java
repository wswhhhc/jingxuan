package com.jingxuan.portfolio.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 申请删除已发布作品的理由。 */
public record V1DeleteRequest(@NotBlank @Size(max = 500) String reason) {
}

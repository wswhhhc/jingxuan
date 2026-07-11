package com.jingxuan.portfolio.api;

import jakarta.validation.constraints.NotNull;

/** 设置作品是否为精选。 */
public record V1FeaturedRequest(@NotNull Boolean featured, String previewUrl) {
}

package com.jingxuan.referencedata.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 标签创建和更新请求。 */
public record V1TagRequest(
        @NotBlank @Size(max = 64) String name,
        @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String type,
        Integer sort
) {
}

package com.jingxuan.referencedata.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 字典项创建和更新请求。 */
public record V1DictRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String type,
        @NotBlank @Size(max = 100) String label,
        @NotBlank @Size(max = 100) String value,
        Integer sort,
        @Size(max = 200) String remark
) {
}

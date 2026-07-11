package com.jingxuan.modules.publish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "精选设置请求")
public class FeaturedRequest {

    @NotNull(message = "作品ID不能为空")
    @Schema(description = "作品ID")
    private Long workId;

    @NotNull(message = "精选标记不能为空")
    @Schema(description = "精选标记 0=取消精选 1=设为精选")
    private Integer featured;

    @Schema(description = "预览地址")
    private String previewUrl;
}

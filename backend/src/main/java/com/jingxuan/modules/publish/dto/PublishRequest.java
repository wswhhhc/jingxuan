package com.jingxuan.modules.publish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "发布请求")
public class PublishRequest {

    @NotNull(message = "作品ID不能为空")
    @Schema(description = "作品ID")
    private Long workId;
}

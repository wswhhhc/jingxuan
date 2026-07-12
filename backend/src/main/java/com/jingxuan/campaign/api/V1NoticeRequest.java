package com.jingxuan.campaign.api;

import jakarta.validation.constraints.NotBlank;

/** v1 保存待办要求请求。 */
public record V1NoticeRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}

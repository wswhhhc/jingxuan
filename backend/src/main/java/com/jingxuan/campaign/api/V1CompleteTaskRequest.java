package com.jingxuan.campaign.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 提交作品后完成待办。 */
public record V1CompleteTaskRequest(@NotBlank @Pattern(regexp = "[0-9]{1,19}") String workId) {
}

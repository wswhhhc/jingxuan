package com.jingxuan.portfolio.api;

import jakarta.validation.constraints.Pattern;

/** 提交作品审核时可关联学生待办。 */
public record V1SubmitWorkRequest(@Pattern(regexp = "[0-9]{1,19}") String taskId) {
    public Long parsedTaskId() { return taskId == null ? null : Long.valueOf(taskId); }
}

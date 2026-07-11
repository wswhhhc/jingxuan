package com.jingxuan.portfolio.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record V1CreateCommentRequest(@NotBlank @Size(max = 1000) String content,
                                     @Pattern(regexp = "[0-9]{1,19}") String parentId,
                                     @Size(max = 50) String guestName) { }

package com.jingxuan.identityaccess.api;

import io.swagger.v3.oas.annotations.media.Schema;

/** v1 AI 导入消息。 */
@Schema(name = "V1AiImportMessage", description = "AI 导入对话消息")
public record V1AiImportMessage(
        String role,
        String content
) {
}

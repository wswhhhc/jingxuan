package com.jingxuan.identityaccess.api;

import com.jingxuan.modules.userimport.dto.AiUserImportResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** v1 AI 解析导入响应。 */
@Schema(name = "V1AiUserImportResponse", description = "AI 解析批量导入用户响应")
public record V1AiUserImportResponse(
        String assistantReply,
        boolean ready,
        List<String> requiredFields,
        List<String> optionalFields,
        List<String> missingFields,
        List<String> assumptions,
        int userCount
) {
}

package com.jingxuan.identityaccess.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** v1 AI 解析导入请求。 */
@Schema(name = "V1AiUserImportRequest", description = "AI 解析批量导入用户请求")
public record V1AiUserImportRequest(
        List<V1AiImportMessage> messages
) {
}

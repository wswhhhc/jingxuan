package com.jingxuan.identityaccess.api;

import java.util.List;

/** v1 批量导入结果。 */
public record V1BatchImportResult(
        int success,
        int failed,
        List<String> errors
) {
}

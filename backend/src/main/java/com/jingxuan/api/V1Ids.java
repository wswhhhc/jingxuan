package com.jingxuan.api;

import com.jingxuan.exception.BusinessException;

/** v1 路径中的雪花 ID 以字符串接收，避免客户端发生数字精度丢失。 */
public final class V1Ids {

    private V1Ids() {
    }

    public static Long parse(String value, String field) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(400, field + " 必须是有效 ID");
        }
    }
}

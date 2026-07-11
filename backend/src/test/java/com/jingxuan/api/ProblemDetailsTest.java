package com.jingxuan.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProblemDetailsTest {

    @Test
    void copiesFieldErrorsAndKeepsOriginalImmutable() {
        ProblemDetails details = ProblemDetails.of(422, "VALIDATION_ERROR", "参数错误", "/api/v1", "id")
                .withFieldErrors(Map.of("title", "不能为空"));

        assertEquals("不能为空", details.fieldErrors().get("title"));
        assertThrows(UnsupportedOperationException.class,
                () -> details.fieldErrors().put("title", "被修改"));
    }
}

package com.jingxuan.portfolio.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V1CreateWorkRequestTest {
    @Test void convertsStringBatchIdWithoutLosingPrecision() {
        var request = new V1CreateWorkRequest("作品", "简介", "Java", null, null, null, "说明", null,
                List.of("9007199254740993"), "9007199254740994");
        var mapped = request.toLegacyRequest();
        assertEquals(9007199254740994L, mapped.getBatchId());
        assertEquals(List.of("9007199254740993"), mapped.getAttachmentIds());
        assertEquals("说明", mapped.getRunDesc());
    }
}

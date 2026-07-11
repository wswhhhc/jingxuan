package com.jingxuan.portfolio.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class V1UpdateWorkRequestTest {
    @Test void preservesAbsentFieldsForPartialUpdate() {
        var mapped = new V1UpdateWorkRequest(null, "新简介", null, null, null, null, null, null, null).toLegacyRequest();
        assertNull(mapped.getTitle());
        assertEquals("新简介", mapped.getSummary());
        assertNull(mapped.getAttachmentIds());
    }
}

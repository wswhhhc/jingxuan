package com.jingxuan.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class V1PageInfoTest {
    @Test void calculatesTotalPages() { assertEquals(3, V1PageInfo.of(2, 10, 21).totalPages()); }
    @Test void rejectsInvalidParameters() { assertThrows(IllegalArgumentException.class, () -> V1PageInfo.of(0, 0, -1)); }
}

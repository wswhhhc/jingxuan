package com.jingxuan.api;

import com.jingxuan.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class V1IdsTest {

    @Test
    void parsesSnowflakeIdWithoutChangingItsValue() {
        assertEquals(9007199254740993L, V1Ids.parse("9007199254740993", "id"));
    }

    @Test
    void rejectsNonNumericIdAsBadRequest() {
        BusinessException error = assertThrows(BusinessException.class, () -> V1Ids.parse("not-an-id", "id"));
        assertEquals(400, error.getCode());
    }
}

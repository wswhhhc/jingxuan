package com.jingxuan.modules.adapter;

import com.jingxuan.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StudentMyRanksApi - 学生排名集成测试")
class StudentMyRanksApiTest extends BaseApiTest {

    @Test
    @DisplayName("已公示批次返回个人排名")
    void getPublishedRanks() {
        ApiResponse resp = studentApi.get("/student/score/my-ranks");
        resp.assertOk();
        assertTrue(resp.getDataNode().isArray());
        assertTrue(resp.getDataNode().size() >= 1, "应至少返回一个已公示批次排名");
    }
}

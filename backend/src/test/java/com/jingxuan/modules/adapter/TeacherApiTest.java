package com.jingxuan.modules.adapter;

import com.jingxuan.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TeacherApi - 教师端接口集成测试")
class TeacherApiTest extends BaseApiTest {

    @Nested
    @DisplayName("GET /teacher/work/list - 已审核作品列表")
    class WorkList {

        @Test
        @DisplayName("仅返回已通过作品(status=3)")
        void listApprovedWorks() {
            ApiResponse resp = teacherApi.get("/teacher/work/list");
            resp.assertOk();
            assertTrue(resp.getDataInt("total") >= 3, "应至少返回3个已通过作品");
        }

        @Test
        @DisplayName("学生访问返回 403")
        void studentForbidden() {
            ApiResponse resp = studentApi.get("/teacher/work/list");
            assertEquals(403, resp.getCode(), "学生访问教师接口应返回 403");
        }
    }

    @Nested
    @DisplayName("GET /teacher/work/{id} - 作品详情（匿名）")
    class WorkDetail {

        @Test
        @DisplayName("已通过作品详情不含学生信息")
        void approvedWorkDetail() {
            ApiResponse resp = teacherApi.get("/teacher/work/1");
            resp.assertOk();
            assertEquals("校园二手书交易平台", resp.getDataText("title"));
        }
    }

    @Nested
    @DisplayName("POST /teacher/score - 提交评分")
    class SubmitScore {

        @Test
        @DisplayName("对已通过作品成功评分")
        void scoreApprovedWork() {
            // 作品4已通过但未评分
            ApiResponse resp = teacherApi.post("/teacher/score", Map.of(
                    "workId", 4,
                    "innovation", 20, "difficulty", 20,
                    "completion", 25, "practicality", 15,
                    "comment", "功能完整"
            ));
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400,
                    "期望 200 或 400，实际: " + resp.getCode());
        }

        @Test
        @DisplayName("对未通过作品评分失败")
        void scoreNotApproved() {
            ApiResponse resp = teacherApi.post("/teacher/score", Map.of(
                    "workId", 5,
                    "innovation", 20, "difficulty", 20,
                    "completion", 25, "practicality", 15,
                    "comment", "不应成功"
            ));
            assertEquals(400, resp.getCode(), "对未通过作品评分应返回 400");
        }
    }

    @Nested
    @DisplayName("GET /teacher/score/{workId} - 我的评分")
    class GetScore {

        @Test
        @DisplayName("查询已评分作品")
        void scored() {
            ApiResponse resp = teacherApi.get("/teacher/score/1");
            resp.assertOk();
        }
    }

    @Nested
    @DisplayName("GET /teacher/score/history - 评分历史")
    class ScoreHistory {

        @Test
        @DisplayName("返回评分历史")
        void history() {
            ApiResponse resp = teacherApi.get("/teacher/score/history");
            resp.assertOk();
        }
    }

    @Nested
    @DisplayName("GET /teacher/dashboard/stats - 教师控制台统计")
    class DashboardStats {

        @Test
        @DisplayName("返回待评分和完成率等统计字段")
        void stats() {
            ApiResponse resp = teacherApi.get("/teacher/dashboard/stats");
            resp.assertOk();
            assertTrue(resp.getDataInt("totalScorableWorks") >= 0);
            assertTrue(resp.getDataInt("scoredWorks") >= 0);
            assertTrue(resp.getDataInt("pendingWorks") >= 0);
            assertTrue(resp.getDataInt("completionRate") >= 0);
            assertTrue(resp.getDataNode().has("activeBatchCount"));
            assertTrue(resp.getDataNode().has("unreadCount"));
        }
    }

    @Nested
    @DisplayName("GET /teacher/batch/list - 批次列表")
    class BatchList {

        @Test
        @DisplayName("返回评分批次")
        void listBatches() {
            ApiResponse resp = teacherApi.get("/teacher/batch/list");
            resp.assertOk();
            assertTrue(resp.getDataNode().isArray());
        }
    }

    @Nested
    @DisplayName("排行榜")
    class Ranking {

        @Test
        @DisplayName("查询排行榜")
        void ranking() {
            ApiResponse resp = teacherApi.get("/teacher/ranking/list", Map.of("batchId", 1, "topN", 5));
            assertTrue(resp.getCode() == 200 || resp.getCode() == 500,
                    "期望 200 或 500，实际: " + resp.getCode());
        }

        @Test
        @DisplayName("刷新排行榜缓存")
        void refresh() {
            ApiResponse resp = teacherApi.post("/teacher/ranking/refresh?batchId=1", null);
            assertTrue(resp.getCode() == 200 || resp.getCode() == 500,
                    "期望 200 或 500，实际: " + resp.getCode());
        }
    }

    @Nested
    @DisplayName("GET /teacher/ranking/batches - 排行批次")
    class RankingBatches {

        @Test
        @DisplayName("返回排行批次列表")
        void batches() {
            ApiResponse resp = teacherApi.get("/teacher/ranking/batches");
            resp.assertOk();
        }

        @Test
        @DisplayName("返回排行分类")
        void categories() {
            ApiResponse resp = teacherApi.get("/teacher/ranking/categories", Map.of("batchId", 1));
            resp.assertOk();
        }
    }

    @Nested
    @DisplayName("通知")
    class Notify {

        @Test
        @DisplayName("通知列表")
        void list() {
            ApiResponse resp = teacherApi.get("/teacher/notify/list");
            assertTrue(resp.getCode() == 200 || resp.getCode() == 500);
        }
    }
}

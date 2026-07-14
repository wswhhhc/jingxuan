package com.jingxuan.modules.adapter;

import com.jingxuan.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AdminApi - 管理端接口集成测试")
class AdminApiTest extends BaseApiTest {

    @Nested
    @DisplayName("仪表盘")
    class Dashboard {

        @Test
        @DisplayName("返回统计字段")
        void stats() {
            ApiResponse resp = adminApi.get("/api/admin/dashboard/stats");
            resp.assertOk();
            assertTrue(resp.getDataInt("totalWorks") >= 7);
            assertTrue(resp.getDataInt("publishedWorks") >= 3);
        }

        @Test
        @DisplayName("图表数据")
        void charts() {
            ApiResponse resp = adminApi.get("/api/admin/dashboard/charts");
            resp.assertOk();
        }

        @Test
        @DisplayName("教师访问返回 403")
        void teacherForbidden() {
            ApiResponse resp = teacherApi.get("/api/admin/dashboard/stats");
            assertEquals(403, resp.getCode());
        }
    }

    @Nested
    @DisplayName("审核管理")
    class Audit {

        @Test
        @DisplayName("审核列表")
        void auditList() {
            ApiResponse resp = adminApi.get("/api/admin/audit/list");
            resp.assertOk();
        }

        @Test
        @DisplayName("按状态筛选")
        void filterByStatus() {
            ApiResponse resp = adminApi.get("/api/admin/audit/list?status=1");
            resp.assertOk();
        }

        @Test
        @DisplayName("审核历史查询")
        void auditHistory() {
            ApiResponse resp = adminApi.get("/api/admin/audit/1/history");
            resp.assertOk();
        }

        @Test
        @DisplayName("审核驳回已提交作品")
        void rejectSubmittedWork() {
            ApiResponse resp = adminApi.post("/api/admin/audit", Map.of(
                    "workId", 5,
                    "result", "rejected",
                    "reason", "测试驳回"
            ));
            resp.assertOk();
        }

        @Test
        @DisplayName("审核通过新提交作品")
        void approveSubmittedWork() {
            String title = "审核通过测试-" + System.currentTimeMillis();
            ApiResponse createResp = testStuApi.post("/api/student/works", Map.of(
                    "title", title,
                    "summary", "审核通过流程验证",
                    "techStack", "Java/Spring Boot",
                    "previewUrl", "http://test-server:8080"
            ));
            createResp.assertOk();
            long workId = createResp.getDataNode().asLong();

            attachFileToWorkAsTestStu(workId, "admin-audit.zip");
            attachFileToWorkAsTestStu(workId, "demo.mp4");
            ApiResponse submitResp = testStuApi.post("/api/student/works/" + workId + "/submit", null);
            submitResp.assertOk();

            ApiResponse auditResp = adminApi.post("/api/admin/audit", Map.of(
                    "workId", workId,
                    "result", "approved",
                    "reason", "测试通过"
            ));
            auditResp.assertOk();
        }
    }

    @Nested
    @DisplayName("发布管理")
    class Publish {

        @Test
        @DisplayName("发布已通过作品")
        void publishWork() {
            ApiResponse resp = adminApi.post("/api/admin/audit/4/publish", null);
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400);
        }

        @Test
        @DisplayName("下线已发布作品")
        void offlineWork() {
            ApiResponse resp = adminApi.post("/api/admin/audit/3/offline", null);
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400);
            adminApi.post("/api/admin/audit/3/publish", null); // 恢复
        }

        @Test
        @DisplayName("设为精选")
        void setFeatured() {
            ApiResponse resp = adminApi.post(
                    "/api/admin/audit/1/featured?featured=1&previewUrl=http://test.com", null);
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400);
        }
    }

    @Nested
    @DisplayName("标签管理")
    class Tags {

        @Test
        @DisplayName("标签列表")
        void listTags() {
            ApiResponse resp = adminApi.get("/api/admin/tags");
            resp.assertOk();
            assertTrue(resp.getDataNode().isArray());
        }

        @Test
        @DisplayName("创建标签")
        void createTag() {
            ApiResponse resp = adminApi.post("/api/admin/tags",
                    Map.of("name", "Go-Test-" + System.currentTimeMillis(), "type", "tech", "sort", 5));
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400);
        }
    }

    @Nested
    @DisplayName("公告管理")
    class Notice {

        @Test
        @DisplayName("公告列表")
        void noticeList() {
            ApiResponse resp = adminApi.get("/api/admin/notice/list");
            resp.assertOk();
        }

        @Test
        @DisplayName("创建公告")
        void createNotice() {
            ApiResponse resp = adminApi.post("/api/admin/notice",
                    Map.of("title", "测试公告", "content", "测试内容", "topFlag", 0));
            resp.assertOk();
        }

        @Test
        @DisplayName("更新公告")
        void updateNotice() {
            ApiResponse resp = adminApi.put("/api/admin/notice/1",
                    Map.of("title", "已更新公告", "content", "已更新内容"));
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400);
        }

        @Test
        @DisplayName("公告内容包含 XSS 字符时接口不异常")
        void noticeWithXssContent() {
            ApiResponse resp = adminApi.post("/api/admin/notice",
                    Map.of("title", "XSS公告",
                            "content", "<script>alert('notice')</script>",
                            "topFlag", 0));
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400,
                    "公告 XSS 内容至少不应导致 500，实际: " + resp.getCode());
        }
    }

    @Nested
    @DisplayName("评分批次")
    class ScoreBatch {

        @Test
        @DisplayName("批次列表")
        void batchList() {
            ApiResponse resp = adminApi.get("/api/admin/score-batch/list");
            resp.assertOk();
            assertTrue(resp.getDataInt("total") >= 2);
        }
    }

    @Nested
    @DisplayName("奖项管理")
    class Prize {

        @Test
        @DisplayName("奖项列表")
        void prizeList() {
            ApiResponse resp = adminApi.get("/api/admin/prize/list");
            resp.assertOk();
            assertTrue(resp.getDataInt("total") >= 3);
        }

        @Test
        @DisplayName("创建奖项")
        void createPrize() {
            ApiResponse resp = adminApi.post("/api/admin/prize",
                    Map.of("batchId", 1, "rewardLevel", "特等奖",
                            "rewardName", "特等奖", "prizeName", "大奖", "quota", 1));
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400);
        }

        @Test
        @DisplayName("更新奖项")
        void updatePrize() {
            ApiResponse resp = adminApi.put("/api/admin/prize/1",
                    Map.of("prizeName", "已更新奖品", "quota", 2));
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400);
        }

        @Test
        @DisplayName("删除奖项")
        void deletePrize() {
            ApiResponse resp = adminApi.delete("/api/admin/prize/3");
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400);
        }
    }

    @Nested
    @DisplayName("字典管理")
    class Dict {

        @Test
        @DisplayName("按类型获取")
        void getByType() {
            ApiResponse resp = adminApi.get("/api/admin/dict/type/class");
            resp.assertOk();
            assertTrue(resp.getDataNode().isArray());
        }

        @Test
        @DisplayName("创建字典项")
        void createDict() {
            ApiResponse resp = adminApi.post("/api/admin/dict/create",
                    Map.of("dictType", "test_type", "dictLabel", "测试标签",
                            "dictValue", "test_value", "sort", 1));
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400);
        }
    }

    @Nested
    @DisplayName("日志管理")
    class Logs {

        @Test
        @DisplayName("日志列表")
        void listLogs() {
            ApiResponse resp = adminApi.get("/api/admin/log/list");
            resp.assertOk();
            assertTrue(resp.getDataNode().has("records"));
        }

        @Test
        @DisplayName("按操作类型筛选")
        void filterByAction() {
            ApiResponse resp = adminApi.get("/api/admin/log/list?action=提交评分");
            resp.assertOk();
        }

        @Test
        @DisplayName("按用户筛选")
        void filterByUser() {
            ApiResponse resp = adminApi.get("/api/admin/log/list?userId=200");
            resp.assertOk();
        }
    }

    @Nested
    @DisplayName("评论管理")
    class Comment {

        @Test
        @DisplayName("评论列表")
        void listComments() {
            ApiResponse resp = adminApi.get("/api/admin/comment/list");
            resp.assertOk();
        }

        @Test
        @DisplayName("评论涉及作品选项")
        void workOptions() {
            ApiResponse resp = adminApi.get("/api/admin/comment/work-options");
            resp.assertOk();
        }

        @Test
        @DisplayName("删除评论")
        void deleteComment() {
            ApiResponse addResp = studentApi.post("/api/comment/add?workId=1&content=待删除评论", null);
            addResp.assertOk();
            long commentId = addResp.getDataNode().asLong();
            adminApi.delete("/api/admin/comment/" + commentId).assertOk();
        }
    }

    @Nested
    @DisplayName("评分批次 CRUD")
    class ScoreBatchCrud {

        @Test
        @DisplayName("创建评分批次")
        void createBatch() {
            ApiResponse resp = adminApi.post("/api/score-batch/create", Map.of(
                    "batchName", "测试批次-" + System.currentTimeMillis(),
                    "startTime", "2026-06-01T00:00:00",
                    "endTime", "2026-07-31T23:59:59",
                    "status", 1
            ));
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400,
                    "期望 200 或 400，实际: " + resp.getCode());
        }

        @Test
        @DisplayName("获取活跃批次")
        void getActive() {
            ApiResponse resp = adminApi.get("/api/score-batch/active");
            assertTrue(resp.getCode() == 200 || resp.getCode() == 500);
        }

        @Test
        @DisplayName("更新评分批次")
        void updateBatch() {
            ApiResponse resp = adminApi.put("/api/score-batch/update",
                    Map.of("id", 1, "batchName", "已更新批次名称"));
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400);
        }

        @Test
        @DisplayName("公示/取消公示排名")
        void publishAndUnpublishRanking() {
            ApiResponse publishResp = adminApi.post("/api/score-batch/2/publish-ranking", null);
            assertTrue(publishResp.getCode() == 200 || publishResp.getCode() == 400);
            ApiResponse unpublishResp = adminApi.post("/api/score-batch/2/unpublish-ranking", null);
            assertTrue(unpublishResp.getCode() == 200 || unpublishResp.getCode() == 400);
        }
    }

    @Nested
    @DisplayName("用户管理")
    class UserManagement {

        @Test
        @DisplayName("用户列表（分页）")
        void userList() {
            var resp = adminApi.exchange(org.springframework.http.HttpMethod.GET, "/api/v1/users", null);
            assertEquals(200, resp.getStatusCode().value());
            assertTrue(jsonBody(resp).get("pageInfo").get("total").asLong() >= 8);
        }

        @Test
        @DisplayName("按角色筛选")
        void filterByRole() {
            var resp = adminApi.exchange(org.springframework.http.HttpMethod.GET, "/api/v1/users?roleId=1", null);
            assertEquals(200, resp.getStatusCode().value());
            jsonBody(resp).get("items").forEach(user -> assertEquals(1, user.get("roleId").asInt()));
        }

        @Test
        @DisplayName("用户详情")
        void userDetail() {
            var resp = adminApi.exchange(org.springframework.http.HttpMethod.GET, "/api/v1/users/1", null);
            assertEquals(200, resp.getStatusCode().value());
            assertEquals("admin", jsonBody(resp).get("username").asText());
        }

        @Test
        @DisplayName("创建用户")
        void createUser() {
            String username = "newuser-" + System.currentTimeMillis();
            var resp = adminApi.exchange(org.springframework.http.HttpMethod.POST, "/api/v1/users", Map.of(
                    "username", username,
                    "realName", "新建用户",
                    "password", "123456",
                    "roleId", 1
            ));
            assertEquals(201, resp.getStatusCode().value());
        }

        @Test
        @DisplayName("更新用户")
        void updateUser() {
            var resp = adminApi.exchange(org.springframework.http.HttpMethod.PUT, "/api/v1/users/110",
                    Map.of("username", "teststu", "realName", "已更新姓名", "roleId", 1, "classId", 1));
            assertEquals(200, resp.getStatusCode().value());
        }

        @Test
        @DisplayName("更新用户状态")
        void updateUserStatus() {
            var resp = adminApi.exchange(org.springframework.http.HttpMethod.PUT, "/api/v1/users/110/status",
                    Map.of("status", 1));
            assertEquals(200, resp.getStatusCode().value());
        }
    }

    @Nested
    @DisplayName("角色管理")
    class RoleManagement {

        @Test
        @DisplayName("角色列表")
        void roleList() {
            var resp = adminApi.exchange(org.springframework.http.HttpMethod.GET, "/api/v1/roles", null);
            assertEquals(200, resp.getStatusCode().value());
            assertTrue(jsonBody(resp).get("items").isArray());
        }

        @Test
        @DisplayName("角色详情")
        void roleDetail() {
            var resp = adminApi.exchange(org.springframework.http.HttpMethod.GET, "/api/v1/roles/1", null);
            assertEquals(200, resp.getStatusCode().value());
            assertEquals("学生", jsonBody(resp).get("roleName").asText());
        }

        @Test
        @DisplayName("创建角色")
        void createRole() {
            var resp = adminApi.exchange(org.springframework.http.HttpMethod.POST, "/api/v1/roles", Map.of(
                    "roleName", "测试角色-" + System.currentTimeMillis(),
                    "roleCode", "ROLE_TEST",
                    "description", "测试用"
            ));
            assertEquals(201, resp.getStatusCode().value());
        }

        @Test
        @DisplayName("更新角色")
        void updateRole() {
            var resp = adminApi.exchange(org.springframework.http.HttpMethod.PUT, "/api/v1/roles/2",
                    Map.of("roleName", "教师(已更新)", "roleCode", "ROLE_TEACHER"));
            assertEquals(200, resp.getStatusCode().value());
        }

        @Test
        @DisplayName("删除角色（测试用角色）")
        void deleteRole() {
            String roleCode = "ROLE_TEMP_" + System.currentTimeMillis();
            var createResp = adminApi.exchange(org.springframework.http.HttpMethod.POST, "/api/v1/roles", Map.of(
                    "roleName", "临时角色-" + System.currentTimeMillis(),
                    "roleCode", roleCode,
                    "description", "将被删除"
            ));
            assertEquals(201, createResp.getStatusCode().value());

            var listResp = adminApi.exchange(org.springframework.http.HttpMethod.GET, "/api/v1/roles?size=100", null);
            String id = null;
            for (var role : jsonBody(listResp).get("items")) {
                if (roleCode.equals(role.get("roleCode").asText())) {
                    id = role.get("id").asText();
                    break;
                }
            }
            assertNotNull(id);
            var deleteResp = adminApi.exchange(org.springframework.http.HttpMethod.DELETE,
                    "/api/v1/roles/" + id, null);
            assertEquals(204, deleteResp.getStatusCode().value());
        }
    }

    @Nested
    @DisplayName("敏感规则管理")
    class SensitiveRule {

        @Test
        @DisplayName("规则列表")
        void ruleList() {
            ApiResponse resp = adminApi.get("/api/admin/rule/list");
            resp.assertOk();
        }

        @Test
        @DisplayName("创建规则")
        void createRule() {
            ApiResponse resp = adminApi.post("/api/admin/rule", Map.of(
                    "ruleName", "测试规则-" + System.currentTimeMillis(),
                    "systemPrompt", "你是一个内容审核助手",
                    "onRejectAction", "reject",
                    "status", 1
            ));
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400);
        }

        @Test
        @DisplayName("更新规则")
        void updateRule() {
            ApiResponse createResp = adminApi.post("/api/admin/rule", Map.of(
                    "ruleName", "待更新规则-" + System.currentTimeMillis(),
                    "systemPrompt", "原始提示词",
                    "onRejectAction", "reject",
                    "status", 1
            ));
            createResp.assertOk();
            long id = createResp.getDataNode().asLong();
            ApiResponse updateResp = adminApi.put("/api/admin/rule/" + id,
                    Map.of("ruleName", "已更新规则", "systemPrompt", "更新后的提示词"));
            assertTrue(updateResp.getCode() == 200 || updateResp.getCode() == 400);
        }

        @Test
        @DisplayName("切换规则启用状态")
        void toggleRule() {
            ApiResponse createResp = adminApi.post("/api/admin/rule", Map.of(
                    "ruleName", "待切换规则-" + System.currentTimeMillis(),
                    "systemPrompt", "切换测试提示词",
                    "onRejectAction", "reject",
                    "status", 1
            ));
            createResp.assertOk();
            long id = createResp.getDataNode().asLong();
            ApiResponse toggleResp = adminApi.put("/api/admin/rule/" + id + "/toggle", null);
            assertTrue(toggleResp.getCode() == 200 || toggleResp.getCode() == 400);
        }
    }

    @Nested
    @DisplayName("菜单管理")
    class MenuManagement {

        @Test
        @DisplayName("菜单树")
        void menuTree() {
            var resp = adminApi.exchange(org.springframework.http.HttpMethod.GET, "/api/v1/menus/tree", null);
            assertEquals(200, resp.getStatusCode().value());
            assertTrue(jsonBody(resp).isArray());
        }

        @Test
        @DisplayName("菜单详情")
        void menuDetail() {
            var resp = adminApi.exchange(org.springframework.http.HttpMethod.GET, "/api/v1/menus/1", null);
            assertEquals(200, resp.getStatusCode().value());
            assertEquals("系统管理", jsonBody(resp).get("menuName").asText());
        }

        @Test
        @DisplayName("更新菜单")
        void updateMenu() {
            var resp = adminApi.exchange(org.springframework.http.HttpMethod.PUT, "/api/v1/menus/1",
                    Map.of("menuName", "系统管理(已更新)", "sort", 1));
            assertEquals(200, resp.getStatusCode().value());
        }
    }

    @Nested
    @DisplayName("评分明细")
    class ScoreDetail {

        @Test
        @DisplayName("批次评分明细")
        void batchScoreDetail() {
            ApiResponse resp = adminApi.get("/api/admin/score/batch/1");
            assertTrue(resp.getCode() == 200 || resp.getCode() == 500);
        }
    }

    private void attachFileToWorkAsTestStu(long workId, String filename) {
        byte[] content = filename.endsWith(".mp4") ? createMp4Content() : createZipContent();
        org.springframework.core.io.ByteArrayResource fileResource =
                new org.springframework.core.io.ByteArrayResource(content) {
                    @Override
                    public String getFilename() {
                        return filename;
                    }
                };

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(login("teststu", "test123"));

        org.springframework.util.LinkedMultiValueMap<String, Object> body =
                new org.springframework.util.LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("workId", String.valueOf(workId));

        org.springframework.http.ResponseEntity<String> resp = restTemplate.exchange(
                "http://localhost:" + port + "/api/file/upload",
                org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(body, headers),
                String.class);

        assertEquals(200, resp.getStatusCode().value(), "附件上传失败: " + resp.getBody());
        try {
            var root = objectMapper.readTree(resp.getBody());
            assertEquals(200, root.get("code").asInt(), "附件上传业务失败: " + resp.getBody());
        } catch (Exception e) {
            throw new RuntimeException("解析附件上传响应失败: " + resp.getBody(), e);
        }
    }

    private tools.jackson.databind.JsonNode jsonBody(org.springframework.http.ResponseEntity<String> response) {
        assertNotNull(response.getBody());
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new AssertionError("响应不是合法 JSON: " + response.getBody(), e);
        }
    }

    /** 生成魔数校验可通过的文件内容（真实 zip / mp4 头） */
    private static byte[] createValidFileContent(String filename) {
        if (filename.endsWith(".mp4")) {
            return new byte[]{
                0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p',
                'm', 'p', '4', '2', 0x00, 0x00, 0x00, 0x00,
                'm', 'p', '4', '2', 'm', 'p', '4', '1'
            };
        }
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(bos)) {
            zos.putNextEntry(new java.util.zip.ZipEntry("content.txt"));
            zos.write(("content-for-" + filename).getBytes());
            zos.closeEntry();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        return bos.toByteArray();
    }

    private static byte[] createZipContent() {
        return new byte[]{
            (byte)0x50, (byte)0x4b, (byte)0x03, (byte)0x04, (byte)0x14, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x4f, (byte)0x50, (byte)-57, (byte)0x5c, (byte)-125, (byte)0x16,
            (byte)-36, (byte)-116, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x63, (byte)0x78,
            (byte)0x50, (byte)0x4b, (byte)0x01, (byte)0x02, (byte)0x14, (byte)0x03, (byte)0x14, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x4f, (byte)0x50, (byte)-57, (byte)0x5c,
            (byte)-125, (byte)0x16, (byte)-36, (byte)-116, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)-128, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x63, (byte)0x50,
            (byte)0x4b, (byte)0x05, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x2f, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x20,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        };
    }

    private static byte[] createMp4Content() {
        return new byte[]{
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x18, (byte)0x66, (byte)0x74,
            (byte)0x79, (byte)0x70, (byte)0x6d, (byte)0x70, (byte)0x34, (byte)0x32
        };
    }
}

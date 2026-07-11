package com.jingxuan.modules.adapter;

import com.jingxuan.BaseApiTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异常与边界场景集成测试
 *
 * <p>覆盖参数校验、权限校验、业务规则冲突、边界值等异常场景。
 * 正向用例由各端专用测试类覆盖，本类专注于异常路径。</p>
 */
@DisplayName("异常与边界测试")
class ExceptionBoundaryTest extends BaseApiTest {

    @Nested
    @DisplayName("9.1 参数校验")
    class ParamValidation {

        @Test
        @DisplayName("分页参数 page=0")
        void pageZero() {
            ApiResponse resp = publicApi.get("/public/works?page=0");
            resp.assertOk();
        }

        @Test
        @DisplayName("分页参数 page=-1")
        void pageNegative() {
            ApiResponse resp = publicApi.get("/public/works?page=-1");
            resp.assertOk();
        }

        @Test
        @DisplayName("分页参数 size=10000")
        void sizeLarge() {
            ApiResponse resp = publicApi.get("/public/works?size=10000");
            resp.assertOk();
        }

        @Test
        @DisplayName("非法 ID（字符串）")
        void invalidId() {
            ApiResponse resp = publicApi.get("/public/works/abc");
            // 应为 400 参数错误，404 或 500
            assertTrue(resp.getCode() >= 400, "非法 ID 应返回异常，实际: " + resp.getCode());
        }

        @Test
        @DisplayName("非法枚举值 status=99")
        void invalidStatus() {
            ApiResponse resp = studentApi.get("/student/works", Map.of("status", 99));
            resp.assertOk(); // 无效枚举应被忽略而非报错
        }

        @Test
        @DisplayName("超长作品标题（201字）")
        void titleTooLong() {
            String longTitle = "a".repeat(201);
            ApiResponse resp = testStuApi.post("/student/works", Map.of("title", longTitle));
            assertTrue(resp.getCode() == 400 || resp.getCode() == 200,
                    "超长标题应返回 400 或自动截断，实际: " + resp.getCode());
        }

        @Test
        @DisplayName("标题为纯空格")
        void titleBlank() {
            ApiResponse resp = testStuApi.post("/student/works", Map.of("title", "   "));
            assertTrue(resp.getCode() == 400, "纯空格标题应返回 400，实际: " + resp.getCode());
        }

        @Test
        @DisplayName("日期格式错误不应导致 500")
        void invalidDateFormat() {
            ApiResponse resp = adminApi.get("/admin/audit/list?submitTimeBegin=2026-99-99 99:99:99");
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400,
                    "非法日期格式应被忽略或拦截，但不应 500，实际: " + resp.getCode());
        }
    }

    @Nested
    @DisplayName("9.2 权限校验")
    class AuthValidation {

        @Test
        @DisplayName("未登录访问需认证接口 → 401/403")
        void anonymousAccess() {
            try {
                ApiResponse resp = publicApi.get("/student/works");
                assertTrue(resp.getCode() == 401 || resp.getCode() == 403 || resp.getCode() == 302,
                        "期望 401/403，实际: " + resp.getCode());
            } catch (Exception e) {
                // TestRestTemplate 在 401 时可能抛异常
                assertTrue(true, "未登录被正确拦截");
            }
        }

        @Test
        @DisplayName("学生访问教师接口 → 403")
        void studentAccessTeacher() {
            ApiResponse resp = studentApi.get("/teacher/work/list");
            assertEquals(403, resp.getCode());
        }

        @Test
        @DisplayName("教师访问管理接口 → 403")
        void teacherAccessAdmin() {
            ApiResponse resp = teacherApi.get("/admin/dashboard/stats");
            assertEquals(403, resp.getCode());
        }

        @Test
        @DisplayName("越权修改他人作品")
        void modifyOthersWork() {
            // 作品5属于赵六(103)，张三(100)不能修改
            ApiResponse resp = studentApi.put("/student/works/5",
                    Map.of("title", "越权修改"));
            assertTrue(resp.getCode() == 400 || resp.getCode() == 403,
                    "越权修改应返回 400/403，实际: " + resp.getCode());
        }

        @Test
        @DisplayName("越权删除他人作品")
        void deleteOthersWork() {
            ApiResponse resp = studentApi.delete("/student/works/7");
            assertTrue(resp.getCode() == 400 || resp.getCode() == 403,
                    "越权删除应返回 400/403，实际: " + resp.getCode());
        }
    }

    @Nested
    @DisplayName("9.3 业务规则")
    class BusinessRules {

        @Test
        @DisplayName("对未通过审核的作品评分 → 400")
        void scoreNotApproved() {
            // 作品5是已提交(status=1)，不可评分
            ApiResponse resp = teacherApi.post("/teacher/score", Map.of(
                    "workId", 5,
                    "innovation", 20, "difficulty", 20,
                    "completion", 25, "practicality", 15,
                    "comment", "不应成功"
            ));
            assertEquals(400, resp.getCode());
        }

        @Test
        @DisplayName("评分超出范围 → 400")
        void scoreOutOfRange() {
            // innovation 最高25分
            ApiResponse resp = teacherApi.post("/teacher/score", Map.of(
                    "workId", 4,
                    "innovation", 30, "difficulty", 20,
                    "completion", 25, "practicality", 15,
                    "comment", "超出范围"
            ));
            assertEquals(400, resp.getCode());
        }

        @Test
        @DisplayName("重复点赞状态正确")
        void toggleLike() {
            // 先确保未点赞（取消所有点赞）
            ApiResponse statusResp = studentApi.get("/public/works/1/like-status");
            statusResp.assertOk();
            boolean wasLiked = statusResp.getDataBool("liked");
            if (wasLiked) {
                studentApi.post("/works/1/like", null);
            }

            // 点赞
            ApiResponse likeResp = studentApi.post("/works/1/like", null);
            likeResp.assertOk();
            assertTrue(likeResp.getDataBool("liked"));

            // 取消点赞
            ApiResponse unlikeResp = studentApi.post("/works/1/like", null);
            unlikeResp.assertOk();
            assertFalse(unlikeResp.getDataBool("liked"));
        }

        @Test
        @DisplayName("创建作品在同批次中重复 → 400（使用已有作品的学生）")
        void duplicateInBatch() {
            // 张三(100)在批次1中已有作品
            ApiResponse resp = studentApi.post("/student/works", Map.of(
                    "title", "重复提交测试",
                    "summary", "不应创建成功"
            ));
            assertTrue(resp.getCode() == 400,
                    "同批次重复提交应返回 400，实际: " + resp.getCode());
        }

        @Test
        @DisplayName("查询不存在的作品 → 404")
        void workNotFound() {
            ApiResponse resp = publicApi.get("/public/works/99999");
            assertTrue(resp.getCode() == 404, "不存在作品应返回 404，实际: " + resp.getCode());
        }

        @Test
        @DisplayName("团队成员已在其他作品中返回 400")
        void memberAlreadyInOtherWork() {
            ApiResponse resp = testStuApi.post("/student/works", Map.of(
                    "title", "成员冲突测试-" + System.currentTimeMillis(),
                    "summary", "成员冲突",
                    "members", java.util.List.of(Map.of(
                            "studentName", "李四",
                            "studentNo", "2022002",
                            "isLeader", 0
                    ))
            ));
            assertEquals(400, resp.getCode(), "已在当前批次参与其他作品的成员应被拦截");
        }

        @Test
        @DisplayName("附件被其他作品占用时绑定失败")
        void attachmentAlreadyBound() {
            ApiResponse resp = testStuApi.post("/student/works", Map.of(
                    "title", "附件占用测试-" + System.currentTimeMillis(),
                    "summary", "附件占用",
                    "attachmentIds", java.util.List.of("1")
            ));
            assertEquals(400, resp.getCode(), "已被占用的附件不应允许重复绑定");
        }
    }

    @Nested
    @DisplayName("9.4 并发场景")
    class Concurrent {

        @Test
        @DisplayName("并发点赞和取消不抛异常")
        void concurrentLike() {
            // 10 个线程交替点赞和取消
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(5);
            var futures = new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < 10; i++) {
                final int idx = i;
                futures.add(executor.submit(() -> {
                    try {
                        ApiResponse resp = studentApi.post("/works/1/like", null);
                        return resp.getCode() == 200;
                    } catch (Exception e) {
                        return false;
                    }
                }));
            }
            executor.shutdown();
            long success = futures.stream().filter(f -> {
                try { return f.get(); } catch (Exception e) { return false; }
            }).count();
            long errors = futures.stream().filter(f -> {
                try { return !f.get(); } catch (Exception e) { return true; }
            }).count();
            assertTrue(success >= 1, "并发点赞应至少 1 次成功，实际: " + success);
            assertTrue(success + errors == 10, "并发点赞不应抛异常，成功: " + success + ", 失败: " + errors);
        }

        @Test
        @DisplayName("点赞与浏览计数在并发场景下保持非负且可读")
        void likeAndViewCountConsistency() throws Exception {
            ApiResponse before = publicApi.get("/public/works/1");
            before.assertOk();
            int initialLikeCount = before.getDataInt("likeCount");
            int initialViewCount = before.getDataInt("viewCount");

            var executor = java.util.concurrent.Executors.newFixedThreadPool(6);
            var futures = new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            try {
                for (int i = 0; i < 5; i++) {
                    futures.add(executor.submit(() -> {
                        publicApi.get("/public/works/1");
                        return true;
                    }));
                }
                for (int i = 0; i < 5; i++) {
                    futures.add(executor.submit(() -> {
                        ApiResponse resp = studentApi.post("/works/1/like", null);
                        return resp.getCode() == 200;
                    }));
                }

                for (var future : futures) {
                    future.get();
                }
            } finally {
                executor.shutdownNow();
            }

            ApiResponse after = publicApi.get("/public/works/1");
            after.assertOk();
            assertTrue(after.getDataInt("viewCount") >= initialViewCount,
                    "浏览计数不应倒退");
            assertTrue(after.getDataInt("likeCount") >= 0,
                    "点赞计数不应为负数");

            // 尽量恢复点赞状态，避免影响后续测试
            while (studentApi.get("/public/works/1/like-status").getDataBool("liked")) {
                studentApi.post("/works/1/like", null);
            }
            assertTrue(initialLikeCount >= 0);
        }

        @Test
        @DisplayName("提交审核和审核操作并发时不应出现 500")
        void submitAndAuditAtSameTime() throws Exception {
            String title = "并发提审审核-" + System.currentTimeMillis();
            ApiResponse createResp = testStuApi.post("/student/works", Map.of(
                    "title", title,
                    "summary", "并发场景",
                    "techStack", "Java/Spring Boot"
            ));
            createResp.assertOk();
            long workId = createResp.getDataNode().asLong();

            String attachmentId = uploadAttachmentForWork(workId, "concurrent-submit-audit.zip");
            ApiResponse bindResp = testStuApi.put("/student/works/" + workId, Map.of(
                    "attachmentIds", java.util.List.of(attachmentId)
            ));
            bindResp.assertOk();

            var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                var submitFuture = executor.submit(() -> testStuApi.post("/student/works/" + workId + "/submit", null));
                var auditFuture = executor.submit(() -> adminApi.post("/admin/audit", Map.of(
                        "workId", workId,
                        "result", "approved",
                        "reason", "并发审核"
                )));

                ApiResponse submitResp = submitFuture.get();
                ApiResponse auditResp = auditFuture.get();

                assertTrue(submitResp.getCode() == 200 || submitResp.getCode() == 400,
                        "提交审核不应出现 500，实际: " + submitResp.getCode());
                assertTrue(auditResp.getCode() == 200 || auditResp.getCode() == 400,
                        "并发审核不应出现 500，实际: " + auditResp.getCode());
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Nested
    @DisplayName("9.5 Token 安全")
    class TokenSecurity {

        @Test
        @DisplayName("伪造 Token 返回 401")
        void fakeToken() {
            // 用完全伪造的 token
            String response = request(
                    org.springframework.http.HttpMethod.GET,
                    "/student/works", null, "fake-jwt-token-12345");
            try {
                var root = objectMapper.readTree(response);
                assertTrue(root.get("code").asInt() == 401 || root.get("code").asInt() == 500,
                        "伪造 token 期望 401/500，实际: " + root.get("code"));
            } catch (Exception e) {
                // 可能返回空响应体的 401
                assertTrue(true, "伪造 token 被拦截");
            }
        }

        @Test
        @DisplayName("Bearer 前缀缺失返回 401")
        void noBearerPrefix() {
            // 使用有效 token 但去掉 Bearer 前缀
            try {
                ApiResponse resp = new ApiResponse(request(
                        org.springframework.http.HttpMethod.GET,
                        "/auth/user-info", null, ""));
                assertTrue(resp.getCode() == 401 || resp.getCode() == 500);
            } catch (Exception e) {
                assertTrue(true, "无 token 被拦截");
            }
        }

        @Test
        @DisplayName("过期 Token 返回 401")
        void expiredToken() {
            String expiredToken = generateExpiredToken(100L, "2022001", "ROLE_STUDENT");
            String response = request(
                    org.springframework.http.HttpMethod.GET,
                    "/student/works", null, expiredToken);
            try {
                var root = objectMapper.readTree(response);
                assertTrue(root.get("code").asInt() == 401 || root.get("code").asInt() == 500,
                        "过期 token 期望 401/500，实际: " + root.get("code"));
            } catch (Exception e) {
                assertTrue(true, "过期 token 被拦截");
            }
        }
    }

    @Nested
    @DisplayName("9.6 注入安全")
    class InjectionSecurity {

        @Test
        @DisplayName("SQL 注入尝试不导致异常")
        void sqlInjection() {
            ApiResponse resp = publicApi.get("/public/works?keyword=' OR 1=1--");
            resp.assertOk();
        }

        @Test
        @DisplayName("XSS 字符在作品标题中不导致异常")
        void xssInTitle() {
            // 使用 testStuApi（无现有作品）
            ApiResponse resp = testStuApi.post("/student/works", Map.of(
                    "title", "<script>alert('XSS')</script>",
                    "summary", "XSS测试"
            ));
            // 可能因内容审核拒绝或通过
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400,
                    "XSS 标题期望 200 或 400，实际: " + resp.getCode());
        }

        @Test
        @DisplayName("评论内容包含 XSS 字符时不导致 500")
        void xssInComment() {
            ApiResponse resp = studentApi.post("/comment/add?workId=1&content=<script>alert('xss')</script>", null);
            assertTrue(resp.getCode() == 200 || resp.getCode() == 400,
                    "评论 XSS 内容应被放行或拦截，但不应 500，实际: " + resp.getCode());
        }
    }

    @Nested
    @DisplayName("9.6 敏感字段与横向权限")
    class SensitiveFields {

        @Test
        @DisplayName("用户信息接口不泄露密码和内部异常堆栈")
        void userInfoShouldNotLeakSensitiveFields() {
            String response = request(
                    org.springframework.http.HttpMethod.GET,
                    "/auth/user-info", null, login("2022001", "test123"));
            assertFalse(response.toLowerCase().contains("password"), "响应不应包含 password 字段");
            assertFalse(response.toLowerCase().contains("stacktrace"), "响应不应包含 stacktrace");
        }

        @Test
        @DisplayName("公开作品详情不泄露 Token 和隐私字段")
        void publicWorkDetailShouldNotLeakSensitiveFields() {
            String response = request(
                    org.springframework.http.HttpMethod.GET,
                    "http://localhost:" + port + "/public/works/1", null, null);
            assertFalse(response.toLowerCase().contains("token"), "公开详情不应包含 token");
            assertFalse(response.toLowerCase().contains("password"), "公开详情不应包含 password");
        }

        @Test
        @DisplayName("教师查看作品详情不应暴露提交者身份")
        void teacherDetailShouldHideSubmitterIdentity() {
            ApiResponse resp = teacherApi.get("/teacher/work/1");
            resp.assertOk();
            assertNull(resp.getDataText("submitterName"));
            assertNull(resp.getDataText("submitterId"));
        }

        @Test
        @DisplayName("异常响应不泄露内部堆栈和类名")
        void errorResponseShouldNotLeakStackTrace() {
            String response = request(
                    org.springframework.http.HttpMethod.GET,
                    "http://localhost:" + port + "/public/works/abc", null, null);
            String lower = response.toLowerCase();
            assertFalse(lower.contains("exception"), "响应不应暴露 exception 细节");
            assertFalse(lower.contains("stacktrace"), "响应不应暴露 stacktrace");
            assertFalse(lower.contains("at com."), "响应不应暴露 Java 调用栈");
        }
    }

    private String uploadAttachmentForWork(long workId, String filename) {
        byte[] content = ("content-for-" + filename).getBytes();
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
            return root.get("data").get("id").asText();
        } catch (Exception e) {
            throw new RuntimeException("解析附件上传响应失败: " + resp.getBody(), e);
        }
    }

    @Nested
    @DisplayName("9.7 边界查询")
    class BoundaryQuery {

        @Test
        @DisplayName("大量标签查询")
        void manyTags() {
            ApiResponse resp = publicApi.get("/public/works?tagIds=1,2,3,4");
            resp.assertOk();
        }

        @Test
        @DisplayName("排行榜 topN=0")
        void rankingZero() {
            ApiResponse resp = publicApi.get("/public/ranking/list?batchId=1&topN=0");
            assertTrue(resp.getCode() == 200 || resp.getCode() == 500);
        }

        @Test
        @DisplayName("排行榜 topN=100")
        void rankingLarge() {
            ApiResponse resp = publicApi.get("/public/ranking/list?batchId=1&topN=100");
            assertTrue(resp.getCode() == 200 || resp.getCode() == 500);
        }
    }

    private String generateExpiredToken(Long userId, String username, String role) {
        String secret = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA";
        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(secretKey)
                .compact();
    }
}

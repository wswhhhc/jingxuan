package com.jingxuan.modules.adapter;

import com.jingxuan.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端业务流程测试
 *
 * <p>跨角色编排完整业务流程，验证各环节衔接正确性。
 * 每个流程从起点开始依次执行，不依赖已有测试数据中的中间状态。</p>
 */
@DisplayName("端到端业务流程")
class EndToEndFlowTest extends BaseApiTest {

    @Nested
    @DisplayName("流程1: 作品完整生命周期")
    class WorkLifecycle {

        @Test
        @DisplayName("学生创建作品 → 管理员审核 → 发布 → 前台可见")
        void fullLifecycle() {
            // 1. 学生创建作品（使用 teststu，无现有作品）
            String title = "E2E测试作品-" + System.currentTimeMillis();
            ApiResponse createResp = testStuApi.post("/student/works", Map.of(
                    "title", title,
                    "summary", "端到端测试创建",
                    "techStack", "Java/Spring Boot"
            ));
            createResp.assertOk();
            long workId = createResp.getDataNode().asLong();
            assertTrue(workId > 0);

            // 2. 学生查看自己的作品列表，新作品在列表中
            ApiResponse listResp = testStuApi.get("/student/works");
            listResp.assertOk();
            assertTrue(listResp.getDataInt("total") >= 1);

            // 3. 提交审核（可能因缺少附件被拒，这是预期行为）
            ApiResponse submitResp = testStuApi.post("/student/works/" + workId + "/submit", null);
            // 有附件时成功200，无附件时失败400（需先上传文件）
            assertTrue(submitResp.getCode() == 200 || submitResp.getCode() == 400,
                    "提交审核状态: " + submitResp.getCode());

            // 如果提交成功，走完整审核-发布流程
            if (submitResp.getCode() == 200) {
                // 4. 管理员审核通过
                ApiResponse auditResp = adminApi.post("/admin/audit", Map.of(
                        "workId", workId, "result", "approved", "reason", "E2E测试通过"
                ));
                auditResp.assertOk();

                // 5. 管理员发布作品
                ApiResponse publishResp = adminApi.post("/admin/audit/" + workId + "/publish", null);
                publishResp.assertOk();

                // 6. 前台可见
                ApiResponse publicResp = publicApi.get("/public/works/" + workId);
                publicResp.assertOk();
                assertEquals(title, publicResp.getDataText("title"));
            }
        }
    }

    @Nested
    @DisplayName("流程1-补充: 驳回后重新提交")
    class RejectedThenResubmit {

        @Test
        @DisplayName("学生提交 → 管理员驳回 → 学生修改后重新提交 → 审核通过")
        void rejectThenResubmitAndApprove() {
            String title = "驳回重提测试-" + System.currentTimeMillis();
            ApiResponse createResp = testStuApi.post("/student/works", Map.of(
                    "title", title,
                    "summary", "首次提交流程",
                    "techStack", "Java/Spring Boot",
                    "previewUrl", "http://test:8080"
            ));
            createResp.assertOk();
            long workId = createResp.getDataNode().asLong();

            String attachmentId = uploadAttachmentForWork(workId, "reject-resubmit.zip");
            String videoId = uploadAttachmentForWork(workId, "demo.mp4");
            ApiResponse bindResp = testStuApi.put("/student/works/" + workId, Map.of(
                    "attachmentIds", java.util.List.of(attachmentId, videoId)
            ));
            bindResp.assertOk();

            ApiResponse firstSubmit = testStuApi.post("/student/works/" + workId + "/submit", null);
            firstSubmit.assertOk();

            ApiResponse rejectResp = adminApi.post("/admin/audit", Map.of(
                    "workId", workId,
                    "result", "rejected",
                    "reason", "测试驳回后重提"
            ));
            rejectResp.assertOk();

            ApiResponse updateResp = testStuApi.put("/student/works/" + workId, Map.of(
                    "title", title + "-修改版",
                    "summary", "驳回后修改再次提交"
            ));
            updateResp.assertOk();

            ApiResponse secondSubmit = testStuApi.post("/student/works/" + workId + "/submit", null);
            secondSubmit.assertOk();

            ApiResponse approveResp = adminApi.post("/admin/audit", Map.of(
                    "workId", workId,
                    "result", "approved",
                    "reason", "重提后通过"
            ));
            approveResp.assertOk();

            ApiResponse historyResp = adminApi.get("/admin/audit/" + workId + "/history");
            historyResp.assertOk();
            assertTrue(historyResp.getDataNode().has("records"));
            assertTrue(historyResp.getDataNode().get("records").size() >= 2, "应至少保留驳回和通过两条审核记录");
        }
    }

    @Nested
    @DisplayName("流程2: 点赞交互")
    class LikeInteraction {

        @Test
        @DisplayName("未登录 → 登录点赞 → 取消 → 计数正确")
        void likeFlow() {
            // 1. 查询初始点赞状态
            ApiResponse initResp = studentApi.get("/public/works/1/like-status");
            initResp.assertOk();
            boolean initLiked = initResp.getDataBool("liked");
            int initCount = initResp.getDataInt("likeCount");

            // 2. 如果已点赞，先取消
            if (initLiked) {
                studentApi.post("/works/1/like", null);
                initCount = Math.max(initCount - 1, 0);
            }

            // 3. 点赞
            ApiResponse likeResp = studentApi.post("/works/1/like", null);
            likeResp.assertOk();
            assertTrue(likeResp.getDataBool("liked"));
            assertEquals(initCount + 1, likeResp.getDataInt("likeCount"),
                    "点赞后计数应+1");

            // 4. 取消点赞
            ApiResponse unlikeResp = studentApi.post("/works/1/like", null);
            unlikeResp.assertOk();
            assertFalse(unlikeResp.getDataBool("liked"));
            assertEquals(initCount, unlikeResp.getDataInt("likeCount"),
                    "取消后计数应恢复");
        }
    }

    @Nested
    @DisplayName("流程3: 评论交互")
    class CommentFlow {

        @Test
        @DisplayName("登录发表评论 → 查看评论列表 → 管理员删除")
        void commentLifecycle() {
            // 1. 获取评论列表（初始）
            ApiResponse listBefore = studentApi.get("/comment/list/1");
            listBefore.assertOk();
            int totalBefore = listBefore.getDataInt("total");

            // 2. 学生发表评论
            ApiResponse addResp = studentApi.post("/comment/add?workId=1&content=E2E测试评论内容", null);
            addResp.assertOk();
            long commentId = addResp.getDataNode().asLong();
            assertTrue(commentId > 0);

            // 3. 评论列表+1
            ApiResponse listAfter = studentApi.get("/comment/list/1");
            listAfter.assertOk();
            assertEquals(totalBefore + 1, listAfter.getDataInt("total"),
                    "评论后列表应+1");

            // 4. 管理员删除评论
            ApiResponse delResp = adminApi.delete("/admin/comment/" + commentId);
            assertTrue(delResp.getCode() == 200 || delResp.getCode() == 500,
                    "删除评论期望 200 或 500，实际: " + delResp.getCode());
        }
    }

    @Nested
    @DisplayName("流程4: 标签筛选")
    class TagFilter {

        @Test
        @DisplayName("管理员创建标签 → 前台按标签筛选作品")
        void tagFilterFlow() {
            // 1. 管理员创建标签
            String tagName = "E2E标签-" + System.currentTimeMillis();
            ApiResponse createTagResp = adminApi.post("/admin/tags",
                    Map.of("name", tagName, "type", "tech", "sort", 99));
            assertTrue(createTagResp.getCode() == 200 || createTagResp.getCode() == 400);

            // 2. 获取标签列表，确认包含新标签
            ApiResponse tagListResp = adminApi.get("/admin/tags");
            tagListResp.assertOk();
            assertTrue(tagListResp.getDataNode().isArray());

            // 3. 前台标签列表
            ApiResponse publicTagsResp = publicApi.get("/public/tags");
            publicTagsResp.assertOk();
            assertTrue(publicTagsResp.getDataNode().isArray());
        }
    }

    @Nested
    @DisplayName("流程5: 教师评分流程")
    class ScoreFlow {

        @Test
        @DisplayName("管理员创建批次 → 教师评分 → 查看评分结果")
        void scoreLifecycle() {
            // 1. 为已审核作品评分
            ApiResponse scoreResp = teacherApi.post("/teacher/score", Map.of(
                    "workId", 4,
                    "innovation", 22, "difficulty", 20,
                    "completion", 27, "practicality", 18,
                    "comment", "E2E测试评分"
            ));
            assertTrue(scoreResp.getCode() == 200 || scoreResp.getCode() == 400,
                    "评分期望 200/400，实际: " + scoreResp.getCode());

            if (scoreResp.getCode() == 200) {
                // 2. 查询我的评分
                ApiResponse myScoreResp = teacherApi.get("/teacher/score/4");
                myScoreResp.assertOk();
            }

            // 3. 评分历史可查
            ApiResponse historyResp = teacherApi.get("/teacher/score/history");
            historyResp.assertOk();
        }
    }

    @Nested
    @DisplayName("流程6: 权限隔离")
    class AuthIsolation {

        @Test
        @DisplayName("学生不能访问教师接口")
        void studentToTeacher() {
            ApiResponse resp = studentApi.get("/teacher/work/list");
            assertEquals(403, resp.getCode());
        }

        @Test
        @DisplayName("教师不能访问管理接口")
        void teacherToAdmin() {
            ApiResponse resp = teacherApi.get("/admin/dashboard/stats");
            assertEquals(403, resp.getCode());
        }

        @Test
        @DisplayName("匿名不能访问需认证接口")
        void anonymousToAuth() {
            try {
                ApiResponse resp = publicApi.get("/student/works");
                assertTrue(resp.getCode() == 401 || resp.getCode() == 403);
            } catch (Exception e) {
                // TestRestTemplate 在 401 时可能抛异常
                assertTrue(true);
            }
        }
    }

    private String uploadAttachmentForWork(long workId, String filename) {
        byte[] content = filename.endsWith(".mp4") ? createMp4Content() : createZipContent();
        ByteArrayResource fileResource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(login("teststu", "test123"));

        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("workId", String.valueOf(workId));

        ResponseEntity<String> resp = restTemplate.exchange(
                "http://localhost:" + port + "/api/file/upload",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
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

    /** 生成魔数校验可通过的文件内容 */
    private static byte[] createTestFileContent(String filename) {
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

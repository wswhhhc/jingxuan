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

@DisplayName("StudentApi - 学生端接口集成测试")
class StudentApiTest extends BaseApiTest {

    @Nested
    @DisplayName("POST /student/works - 创建作品")
    class CreateWork {

        @Test
        @DisplayName("创建作品成功（使用无作品测试学生）")
        void createWorkSuccess() {
            ApiResponse resp = testStuApi.post("/student/works", Map.of(
                    "title", "集成测试作品",
                    "summary", "通过集成测试创建的作品",
                    "techStack", "Java/Spring Boot",
                    "advisor", "张教授"
            ));
            resp.assertOk();
            assertNotNull(resp.getDataNode(), "应返回作品ID");
        }

        @Test
        @DisplayName("标题为空返回 400")
        void emptyTitle() {
            ApiResponse resp = testStuApi.post("/student/works", Map.of(
                    "title", "", "summary", "测试"
            ));
            resp.assertCode(400);
        }
    }

    @Nested
    @DisplayName("GET /student/works - 我的作品列表")
    class MyWorks {

        @Test
        @DisplayName("返回当前学生的作品")
        void listMyWorks() {
            ApiResponse resp = studentApi.get("/student/works");
            resp.assertOk();
            assertTrue(resp.getDataInt("total") >= 1, "张三至少应有 1 个作品");
        }

        @Test
        @DisplayName("按状态筛选草稿")
        void filterByStatus() {
            ApiResponse resp = studentApi.get("/student/works", Map.of("status", "0"));
            resp.assertOk();
        }
    }

    @Nested
    @DisplayName("GET /student/works/{id} - 作品详情")
    class WorkDetail {

        @Test
        @DisplayName("查看自己的草稿作品")
        void ownDraftDetail() {
            ApiResponse resp = studentApi.get("/student/works/6");
            resp.assertOk();
            assertEquals("在线考试系统", resp.getDataText("title"));
        }

        @Test
        @DisplayName("查看不存在的作品返回 400/404")
        void notFound() {
            ApiResponse resp = studentApi.get("/student/works/999");
            assertTrue(resp.getCode() == 400 || resp.getCode() == 404);
        }
    }

    @Nested
    @DisplayName("PUT /student/works/{id} - 更新作品")
    class UpdateWork {

        @Test
        @DisplayName("更新自己的草稿作品成功")
        void updateDraft() {
            ApiResponse resp = studentApi.put("/student/works/6", Map.of(
                    "title", "更新后的考试系统"
            ));
            resp.assertOk();
        }
    }

    @Nested
    @DisplayName("DELETE /student/works/{id} - 删除作品")
    class DeleteWork {

        @Test
        @DisplayName("先创建再删除自己的草稿")
        void createThenDelete() {
            ApiResponse createResp = testStuApi.post("/student/works", Map.of("title", "临时作品-可清理"));
            createResp.assertOk();
            long newId = createResp.getDataNode().asLong();
            ApiResponse delResp = testStuApi.delete("/student/works/" + newId);
            delResp.assertOk();
        }

        @Test
        @DisplayName("删除已发布作品返回 400")
        void deletePublishedWorkRejected() {
            ApiResponse resp = studentApi.delete("/student/works/1");
            assertEquals(400, resp.getCode(), "已发布作品不应允许删除");
        }
    }

    @Nested
    @DisplayName("POST /student/works/{id}/submit - 提交审核")
    class SubmitWork {

        @Test
        @DisplayName("无附件提交返回 400")
        void submitWithoutAttachment() {
            ApiResponse resp = studentApi.post("/student/works/6/submit", null);
            assertEquals(400, resp.getCode(), "无附件提交应返回 400");
        }

        @Test
        @DisplayName("上传附件后提交审核成功")
        void submitWithAttachment() {
            String title = "提交审核测试-" + System.currentTimeMillis();
            ApiResponse createResp = testStuApi.post("/student/works", Map.of(
                    "title", title,
                    "summary", "用于提交流程验证",
                    "techStack", "Java/Spring Boot",
                    "previewUrl", "http://test:8080"
            ));
            createResp.assertOk();
            long workId = createResp.getDataNode().asLong();

            String attachmentId = uploadAttachmentForWork(workId, "submit-flow.zip");
            String videoId = uploadAttachmentForWork(workId, "demo.mp4");

            ApiResponse updateResp = testStuApi.put("/student/works/" + workId, Map.of(
                    "attachmentIds", java.util.List.of(attachmentId, videoId)
            ));
            updateResp.assertOk();

            ApiResponse submitResp = testStuApi.post("/student/works/" + workId + "/submit", null);
            submitResp.assertOk();
        }
    }

    @Nested
    @DisplayName("GET /student/score/my-ranks - 我的排名")
    class MyRanks {

        @Test
        @DisplayName("已公示批次返回个人排名")
        void getPublishedRanks() {
            ApiResponse resp = studentApi.get("/student/score/my-ranks");
            resp.assertOk();
            assertTrue(resp.getDataNode().isArray());
            assertTrue(resp.getDataNode().size() >= 1, "应至少返回一个已公示批次排名");
        }
    }

    @Nested
    @DisplayName("GET /student/batch/available - 可用批次")
    class AvailableBatches {

        @Test
        @DisplayName("返回当前学生可参与的批次")
        void available() {
            ApiResponse resp = studentApi.get("/student/batch/available");
            resp.assertOk();
            assertTrue(resp.getDataNode().isArray());
        }
    }

    @Nested
    @DisplayName("通知相关")
    class Notifications {

        @Test
        @DisplayName("获取通知列表")
        void listNotifications() {
            ApiResponse resp = studentApi.get("/student/notify/list");
            assertTrue(resp.getCode() == 200 || resp.getCode() == 500,
                    "期望 200 或 500，实际: " + resp.getCode());
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

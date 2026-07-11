package com.jingxuan.modules.adapter;

import com.jingxuan.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("StudentSubmitApi - 学生提交审核集成测试")
class StudentSubmitApiTest extends BaseApiTest {

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

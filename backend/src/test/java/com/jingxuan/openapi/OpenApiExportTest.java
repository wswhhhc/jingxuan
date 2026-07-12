package com.jingxuan.openapi;

import com.jingxuan.BaseApiTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 导出 OpenAPI 规格到项目 openapi/ 目录。 */
@Tag("integration")
class OpenApiExportTest extends BaseApiTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exportOpenApiSpec() throws IOException {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/v3/api-docs/v1", String.class);
        assertTrue(response.getStatusCode().is2xxSuccessful(),
                "OpenAPI 端点应返回 200，实际：" + response.getStatusCode());

        String json = response.getBody();
        assertTrue(json != null && json.contains("/api/v1/"),
                "OpenAPI 响应应包含 v1 端点定义");

        Path outputPath = Paths.get("..", "openapi", "jingxuan-v1.json").normalize();
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, json);

        // 也输出一份路径计数报告
        long pathCount = json.chars().filter(c -> c == '/').count();
        System.out.println("OpenAPI 规格已导出到: " + outputPath.toAbsolutePath());
        System.out.println("包含 " + pathCount + " 个路径定义");

        assertTrue(json.contains("/api/v1/notices"), "应包含 notices 端点");
        assertTrue(json.contains("/api/v1/leaderboards"), "应包含 leaderboards 端点");
        assertTrue(json.contains("/api/v1/prizes"), "应包含 prizes 端点");
        assertTrue(json.contains("/api/v1/users"), "应包含 users 管理端点");
        assertTrue(json.contains("/api/v1/roles"), "应包含 roles 管理端点");
        assertTrue(json.contains("/api/v1/menus"), "应包含 menus 管理端点");
        assertTrue(json.contains("/api/v1/moderation"), "应包含 moderation 端点");
        assertTrue(json.contains("/api/v1/audit-logs"), "应包含 audit-logs 端点");
        assertTrue(json.contains("/api/v1/dashboard"), "应包含 dashboard 端点");
    }
}

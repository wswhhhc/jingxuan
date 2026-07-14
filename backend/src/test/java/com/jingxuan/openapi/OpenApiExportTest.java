package com.jingxuan.openapi;

import com.jingxuan.BaseApiTest;
import com.jingxuan.api.ApiPaths;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 导出 OpenAPI 规格到测试临时目录，避免测试污染工作区。 */
@Tag("integration")
class OpenApiExportTest extends BaseApiTest {

    private static final Set<String> HTTP_METHODS = Set.of(
            "delete", "get", "head", "options", "patch", "post", "put", "trace");
    private static final Set<String> COMMON_PROBLEM_STATUS_CODES = Set.of(
            "400", "404", "405", "406", "422", "500");

    @TempDir
    Path tempDir;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exportOpenApiSpec() throws IOException {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/v3/api-docs", String.class);
        assertTrue(response.getStatusCode().is2xxSuccessful(),
                "OpenAPI 端点应返回 200，实际：" + response.getStatusCode());

        String json = response.getBody();
        assertTrue(json != null && json.contains("/api/v1/"),
                "OpenAPI 响应应包含 v1 端点定义");
        JsonNode specification = objectMapper.readTree(json);

        Path outputPath = tempDir.resolve("jingxuan-v1.json");
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

        assertProblemDetailsContract(specification);
        assertLongSerializationContract(specification);
    }

    private static void assertProblemDetailsContract(JsonNode specification) {
        JsonNode problemDetails = specification.at("/components/schemas/ProblemDetails");
        assertTrue(problemDetails.isObject(), "components.schemas 应包含 ProblemDetails");
        assertEquals("string", problemDetails.at("/properties/fieldErrors/additionalProperties/type").asText(),
                "fieldErrors 的值必须与 Map<String, String> 一致");

        int operationCount = 0;
        for (var pathEntry : specification.get("paths").properties()) {
            if (!pathEntry.getKey().startsWith("/api/v1")) {
                continue;
            }
            for (var operationEntry : pathEntry.getValue().properties()) {
                if (!HTTP_METHODS.contains(operationEntry.getKey())) {
                    continue;
                }
                operationCount++;
                String method = operationEntry.getKey().toUpperCase();
                String path = pathEntry.getKey();
                JsonNode operation = operationEntry.getValue();
                for (String statusCode : COMMON_PROBLEM_STATUS_CODES) {
                    assertProblemResponse(operation, method, path, statusCode);
                }
                for (var responseEntry : operation.get("responses").properties()) {
                    if (responseEntry.getKey().matches("[45]\\d{2}")) {
                        assertProblemResponse(operation, method, path, responseEntry.getKey());
                    }
                }

                if (ApiPaths.isPublicV1Operation(method, path)) {
                    assertTrue(operation.has("security") && operation.get("security").isArray()
                                    && operation.get("security").isEmpty(),
                            () -> method + " " + path + " 应显式声明 security: []");
                } else {
                    assertProblemResponse(operation, method, path, "401");
                    assertProblemResponse(operation, method, path, "403");
                }

                if (operation.has("requestBody")) {
                    assertProblemResponse(operation, method, path, "415");
                } else {
                    assertTrue(!operation.get("responses").has("415"),
                            () -> method + " " + path + " 没有请求体，不应声明 415");
                }

                if (ApiPaths.isRateLimitedV1Operation(method, path)) {
                    assertProblemResponse(operation, method, path, "429");
                } else {
                    assertTrue(!operation.get("responses").has("429"),
                            () -> method + " " + path + " 不受限流策略约束，不应声明 429");
                }
            }
        }
        assertTrue(operationCount >= 80, "应校验全部 V1 操作，实际仅发现 " + operationCount + " 个");

        JsonNode login = specification.get("paths").get("/api/v1/auth/login").get("post");
        assertResponseSchema(login, "POST", "/api/v1/auth/login", "200",
                "#/components/schemas/V1LoginResponse");
        assertProblemResponse(login, "POST", "/api/v1/auth/login", "401");
        assertProblemResponse(login, "POST", "/api/v1/auth/login", "403");
        assertNoResponse(login, "POST", "/api/v1/auth/login", "429");
        assertNoResponse(login, "POST", "/api/v1/auth/login", "503");

        JsonNode refresh = specification.get("paths").get("/api/v1/auth/refresh").get("post");
        assertResponseSchema(refresh, "POST", "/api/v1/auth/refresh", "200",
                "#/components/schemas/V1LoginResponse");
        assertProblemResponse(refresh, "POST", "/api/v1/auth/refresh", "401");
        assertProblemResponse(refresh, "POST", "/api/v1/auth/refresh", "403");
        assertNoResponse(refresh, "POST", "/api/v1/auth/refresh", "429");
        assertNoResponse(refresh, "POST", "/api/v1/auth/refresh", "503");

        JsonNode emailVerification = specification.get("paths")
                .get("/api/v1/auth/email-verifications").get("post");
        assertProblemResponse(emailVerification, "POST", "/api/v1/auth/email-verifications", "429");
        assertProblemResponse(emailVerification, "POST", "/api/v1/auth/email-verifications", "503");
        assertNoResponse(emailVerification, "POST", "/api/v1/auth/email-verifications", "401");
        assertNoResponse(emailVerification, "POST", "/api/v1/auth/email-verifications", "403");

        JsonNode aiParse = specification.get("paths").get("/api/v1/users/batch/ai-parse").get("post");
        assertProblemResponse(aiParse, "POST", "/api/v1/users/batch/ai-parse", "429");
        assertProblemResponse(aiParse, "POST", "/api/v1/users/batch/ai-parse", "503");

        JsonNode showcase = specification.get("paths").get("/api/v1/showcase/works").get("get");
        assertProblemResponse(showcase, "GET", "/api/v1/showcase/works", "429");
        assertNoResponse(showcase, "GET", "/api/v1/showcase/works", "503");
        assertNoResponse(showcase, "GET", "/api/v1/showcase/works", "401");
        assertNoResponse(showcase, "GET", "/api/v1/showcase/works", "403");
    }

    private static void assertProblemResponse(JsonNode operation, String method, String path, String statusCode) {
        JsonNode response = operation.get("responses").get(statusCode);
        assertNotNull(response, () -> method + " " + path + " 缺少 " + statusCode + " Problem Details 响应");
        assertEquals("#/components/schemas/ProblemDetails",
                response.at("/content/application~1problem+json/schema/$ref").asText(),
                () -> method + " " + path + " 的 " + statusCode + " 应使用 application/problem+json");
    }

    private static void assertNoResponse(JsonNode operation, String method, String path, String statusCode) {
        assertTrue(!operation.get("responses").has(statusCode),
                () -> method + " " + path + " 不应声明 " + statusCode + " 响应");
    }

    private static void assertResponseSchema(JsonNode operation, String method, String path,
                                             String statusCode, String expectedRef) {
        JsonNode response = operation.get("responses").get(statusCode);
        assertNotNull(response, () -> method + " " + path + " 缺少 " + statusCode + " 响应");
        JsonNode content = response.get("content");
        assertNotNull(content, () -> method + " " + path + " 的 " + statusCode + " 缺少响应内容");
        boolean schemaFound = false;
        for (var mediaType : content.properties()) {
            if (expectedRef.equals(mediaType.getValue().at("/schema/$ref").asText())) {
                schemaFound = true;
                break;
            }
        }
        assertTrue(schemaFound,
                () -> method + " " + path + " 的 " + statusCode + " 应引用 " + expectedRef);
    }

    private static void assertLongSerializationContract(JsonNode specification) {
        assertEquals("string",
                specification.at("/components/schemas/ScoreSubmitRequest/properties/workId/type").asText(),
                "boxed Long 请求字段必须与 Jackson 一致导出为 string");
        assertEquals("string",
                specification.at("/components/schemas/WorkMemberDTO/properties/id/type").asText());
        assertEquals("string",
                specification.at("/components/schemas/WorkMemberDTO/properties/studentId/type").asText());
        assertEquals("string",
                specification.at("/components/schemas/V1LoginResponse/properties/expiresIn/type").asText(),
                "非 ID 的 boxed Long 也必须反映真实 JSON 字符串序列化");

        JsonNode leaderboard = specification.get("paths")
                .get("/api/v1/leaderboards").get("get");
        assertEquals("string", parameterSchema(leaderboard, "batchId").get("type").asText());
        assertEquals("string", specification.get("paths")
                .get("/api/v1/roles/{id}/menus").get("get")
                .at("/responses/200/content/*~1*/schema/items/type").asText());
        assertEquals("string", specification.get("paths")
                .get("/api/v1/roles/{id}/menus").get("put")
                .at("/requestBody/content/application~1json/schema/items/type").asText());

        assertEquals("integer",
                specification.at("/components/schemas/V1DashboardStats/properties/totalWorks/type").asText(),
                "primitive long 统计值应继续导出为 integer");
        assertEquals("int64",
                specification.at("/components/schemas/V1DashboardStats/properties/totalWorks/format").asText());
    }

    private static JsonNode parameterSchema(JsonNode operation, String name) {
        for (JsonNode parameter : operation.get("parameters")) {
            if (name.equals(parameter.get("name").asText())) {
                return parameter.get("schema");
            }
        }
        throw new AssertionError("缺少 OpenAPI 参数：" + name);
    }
}

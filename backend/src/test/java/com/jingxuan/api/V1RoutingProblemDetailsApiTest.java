package com.jingxuan.api;

import com.jingxuan.BaseApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证路由层错误也按 API 版本保持响应契约。 */
class V1RoutingProblemDetailsApiTest extends BaseApiTest {

    @Test
    void authenticatedMissingV1RouteReturnsProblemDetails() throws Exception {
        HttpHeaders headers = authenticatedHeaders("routing-404");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/route-does-not-exist", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertEquals(404, response.getStatusCode().value());
        assertProblemJson(response);
        assertEquals("routing-404", response.getHeaders().getFirst(RequestIdFilter.HEADER));
        var body = objectMapper.readTree(response.getBody());
        assertEquals(404, body.get("status").asInt());
        assertEquals("NOT_FOUND", body.get("code").asText());
        assertEquals("/api/v1/route-does-not-exist", body.get("instance").asText());
        assertEquals("routing-404", body.get("requestId").asText());
    }

    @Test
    void authenticatedUnsupportedV1MethodReturnsProblemDetails() throws Exception {
        HttpHeaders headers = authenticatedHeaders("routing-405");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/me/works", HttpMethod.PATCH,
                new HttpEntity<>(headers), String.class);

        assertEquals(405, response.getStatusCode().value());
        assertProblemJson(response);
        assertEquals("routing-405", response.getHeaders().getFirst(RequestIdFilter.HEADER));
        var body = objectMapper.readTree(response.getBody());
        assertEquals(405, body.get("status").asInt());
        assertEquals("METHOD_NOT_ALLOWED", body.get("code").asText());
        assertEquals("/api/v1/me/works", body.get("instance").asText());
        assertEquals("routing-405", body.get("requestId").asText());
    }

    @Test
    void authenticatedMissingLegacyRouteKeepsResultEnvelope() throws Exception {
        HttpHeaders headers = authenticatedHeaders("legacy-routing-404");

        ResponseEntity<String> response = restTemplate.exchange(
                "/route-does-not-exist", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertEquals(404, response.getStatusCode().value());
        assertLegacyJson(response);
        var body = objectMapper.readTree(response.getBody());
        assertEquals(404, body.get("code").asInt());
        assertEquals("请求的资源不存在", body.get("message").asText());
    }

    @Test
    void authenticatedUnsupportedLegacyMethodKeepsResultEnvelope() throws Exception {
        HttpHeaders headers = authenticatedHeaders("legacy-routing-405");

        ResponseEntity<String> response = restTemplate.exchange(
                "/student/works", HttpMethod.PATCH,
                new HttpEntity<>(headers), String.class);

        assertEquals(405, response.getStatusCode().value());
        assertLegacyJson(response);
        var body = objectMapper.readTree(response.getBody());
        assertEquals(405, body.get("code").asInt());
        assertEquals("请求方法不支持: PATCH", body.get("message").asText());
    }

    private HttpHeaders authenticatedHeaders(String requestId) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(login("teststu", "test123"));
        headers.set(RequestIdFilter.HEADER, requestId);
        return headers;
    }

    private void assertProblemJson(ResponseEntity<String> response) {
        MediaType contentType = response.getHeaders().getContentType();
        assertNotNull(contentType);
        assertTrue(MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(contentType),
                () -> "期望 application/problem+json，实际为 " + contentType);
    }

    private void assertLegacyJson(ResponseEntity<String> response) {
        MediaType contentType = response.getHeaders().getContentType();
        assertNotNull(contentType);
        assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(contentType),
                () -> "期望 application/json，实际为 " + contentType);
    }
}

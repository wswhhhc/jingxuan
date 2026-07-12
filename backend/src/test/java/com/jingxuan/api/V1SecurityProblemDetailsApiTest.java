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

/** 验证 Spring Security 层也遵守 v1 Problem Details 错误契约。 */
class V1SecurityProblemDetailsApiTest extends BaseApiTest {

    @Test
    void unauthenticatedV1RequestReturnsProblemDetailsWithRequestId() throws Exception {
        HttpHeaders headers = jsonHeaders();
        headers.set(RequestIdFilter.HEADER, "security-401");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/me/works", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(401, response.getStatusCode().value());
        assertProblemJson(response);
        assertEquals("security-401", response.getHeaders().getFirst(RequestIdFilter.HEADER));
        var body = objectMapper.readTree(response.getBody());
        assertEquals(401, body.get("status").asInt());
        assertEquals("UNAUTHENTICATED", body.get("code").asText());
        assertEquals("security-401", body.get("requestId").asText());
    }

    @Test
    void forbiddenV1RequestReturnsProblemDetailsWithRequestId() throws Exception {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(login("teststu", "test123"));
        headers.set(RequestIdFilter.HEADER, "security-403");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/tags/1/deletion-impact", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertEquals(403, response.getStatusCode().value());
        assertProblemJson(response);
        assertEquals("security-403", response.getHeaders().getFirst(RequestIdFilter.HEADER));
        var body = objectMapper.readTree(response.getBody());
        assertEquals(403, body.get("status").asInt());
        assertEquals("FORBIDDEN", body.get("code").asText());
        assertEquals("security-403", body.get("requestId").asText());
    }

    private void assertProblemJson(ResponseEntity<String> response) {
        MediaType contentType = response.getHeaders().getContentType();
        assertNotNull(contentType);
        assertTrue(MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(contentType),
                () -> "期望 application/problem+json，实际为 " + contentType);
    }
}

package com.jingxuan.api;

import com.jingxuan.BaseApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 v1 公开路由边界与客户端输入错误契约。 */
class V1PublicAndInputContractApiTest extends BaseApiTest {

    @Test
    void anonymousUsersCanReadPublicLeaderboards() throws Exception {
        ResponseEntity<String> ranking = exchangeWithoutToken(
                HttpMethod.GET, "/api/v1/leaderboards?batchId=1", null, "public-ranking");
        ResponseEntity<String> categories = exchangeWithoutToken(
                HttpMethod.GET, "/api/v1/leaderboards/categories?batchId=1", null, "public-categories");

        assertEquals(200, ranking.getStatusCode().value());
        assertEquals(200, categories.getStatusCode().value());
    }

    @Test
    void anonymousUsersCanReadPublishedNoticesButNotAdminNoticeList() throws Exception {
        ResponseEntity<String> published = exchangeWithoutToken(
                HttpMethod.GET, "/api/v1/notices/published?page=1&pageSize=10", null, "public-notices");
        ResponseEntity<String> missingDetail = exchangeWithoutToken(
                HttpMethod.GET, "/api/v1/notices/999999", null, "public-notice-detail");
        ResponseEntity<String> adminList = exchangeWithoutToken(
                HttpMethod.GET, "/api/v1/notices", null, "protected-notices");

        assertEquals(200, published.getStatusCode().value());
        assertEquals(404, missingDetail.getStatusCode().value());
        assertProblem(missingDetail, 404, "NOT_FOUND", "public-notice-detail");
        assertEquals(401, adminList.getStatusCode().value());
    }

    @Test
    void anonymousUsersCannotReadDraftNoticeDetails() throws Exception {
        ResponseEntity<String> created = adminApi.exchange(
                HttpMethod.POST,
                "/api/v1/notices",
                Map.of(
                        "title", "仅管理员可见的草稿公告",
                        "content", "草稿内容不应通过公开详情接口泄露",
                        "publishDirectly", false,
                        "targetScope", "all"));
        assertEquals(201, created.getStatusCode().value());
        String noticeId = objectMapper.readTree(created.getBody()).get("id").asText();

        ResponseEntity<String> response = exchangeWithoutToken(
                HttpMethod.GET, "/api/v1/notices/" + noticeId, null, "public-draft-notice");

        assertProblem(response, 404, "NOT_FOUND", "public-draft-notice");
    }

    @Test
    void anonymousUsersCanReadPublishedNoticeDetails() throws Exception {
        ResponseEntity<String> created = adminApi.exchange(
                HttpMethod.POST,
                "/api/v1/notices",
                Map.of(
                        "title", "公开公告",
                        "content", "已发布公告可以通过公开详情接口读取",
                        "publishDirectly", true,
                        "targetScope", "all"));
        assertEquals(201, created.getStatusCode().value());
        String noticeId = objectMapper.readTree(created.getBody()).get("id").asText();

        ResponseEntity<String> response = exchangeWithoutToken(
                HttpMethod.GET, "/api/v1/notices/" + noticeId, null, "public-published-notice");

        assertEquals(200, response.getStatusCode().value());
        var notice = objectMapper.readTree(response.getBody());
        assertEquals(noticeId, notice.get("id").asText());
        assertEquals("PUBLISHED", notice.get("status").asText());
    }

    @Test
    void adjacentPersonalLeaderboardRouteRemainsProtected() {
        ResponseEntity<String> response = exchangeWithoutToken(
                HttpMethod.GET, "/api/v1/leaderboards/me", null, "protected-my-ranking");

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void missingRequiredQueryParameterReturnsBadRequestProblem() throws Exception {
        ResponseEntity<String> response = adminApi.exchange(
                HttpMethod.GET, "/api/v1/leaderboards", null);

        assertProblem(response, 400, "BAD_REQUEST", null);
    }

    @Test
    void malformedJsonReturnsBadRequestProblem() throws Exception {
        ResponseEntity<String> response = exchangeWithoutToken(
                HttpMethod.POST, "/api/v1/auth/login", "{\"username\":", "malformed-json");

        assertProblem(response, 400, "BAD_REQUEST", "malformed-json");
    }

    @Test
    void unsupportedRequestMediaTypeReturnsUnsupportedMediaTypeProblem() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setAccept(List.of(MediaType.APPLICATION_PROBLEM_JSON));
        headers.set(RequestIdFilter.HEADER, "unsupported-media-type");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>("not-json", headers), String.class);

        assertProblem(response, 415, "UNSUPPORTED_MEDIA_TYPE", "unsupported-media-type");
    }

    @Test
    void anonymousPublicErrorDispatchKeepsOriginalV1ProblemContract() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.TEXT_PLAIN));
        headers.set(RequestIdFilter.HEADER, "public-not-acceptable");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/classes", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertProblem(response, 406, "NOT_ACCEPTABLE", "public-not-acceptable");
        assertEquals("/api/v1/classes", objectMapper.readTree(response.getBody()).get("instance").asText());
    }

    @Test
    void queryParameterTypeMismatchReturnsBadRequestProblem() throws Exception {
        ResponseEntity<String> response = adminApi.exchange(
                HttpMethod.GET, "/api/v1/leaderboards?batchId=not-a-number", null);

        assertProblem(response, 400, "BAD_REQUEST", null);
    }

    @Test
    void queryParameterConstraintViolationReturnsValidationProblem() throws Exception {
        ResponseEntity<String> response = exchangeWithoutToken(
                HttpMethod.GET, "/api/v1/tags?type=!", null, "invalid-tag-type");

        assertProblem(response, 422, "VALIDATION_ERROR", "invalid-tag-type");
    }

    private ResponseEntity<String> exchangeWithoutToken(
            HttpMethod method, String path, Object body, String requestId) {
        HttpHeaders headers = jsonHeaders();
        headers.set(RequestIdFilter.HEADER, requestId);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private void assertProblem(
            ResponseEntity<String> response, int status, String code, String expectedRequestId) throws Exception {
        assertEquals(status, response.getStatusCode().value());
        MediaType contentType = response.getHeaders().getContentType();
        assertNotNull(contentType);
        assertTrue(MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(contentType),
                () -> "期望 application/problem+json，实际为 " + contentType);
        var problem = objectMapper.readTree(response.getBody());
        assertEquals(status, problem.get("status").asInt());
        assertEquals(code, problem.get("code").asText());
        if (expectedRequestId != null) {
            assertEquals(expectedRequestId, problem.get("requestId").asText());
            assertEquals(expectedRequestId, response.getHeaders().getFirst(RequestIdFilter.HEADER));
        }
    }
}

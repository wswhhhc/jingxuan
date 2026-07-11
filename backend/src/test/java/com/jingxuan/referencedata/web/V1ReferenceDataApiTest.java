package com.jingxuan.referencedata.web;

import com.jingxuan.BaseApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 公共参考数据接口的 Testcontainers HTTP 验证。 */
class V1ReferenceDataApiTest extends BaseApiTest {

    @Test
    void classesAndTagsArePublicAndUseStringIds() throws Exception {
        ResponseEntity<String> classes = restTemplate.exchange("/api/v1/classes", HttpMethod.GET, null, String.class);
        assertEquals(200, classes.getStatusCode().value());
        var classItems = objectMapper.readTree(classes.getBody());
        assertEquals("1", classItems.get(0).get("id").asText());
        assertEquals("class", classItems.get(0).get("type").asText());

        ResponseEntity<String> tags = restTemplate.exchange("/api/v1/tags?type=tech", HttpMethod.GET, null, String.class);
        assertEquals(200, tags.getStatusCode().value());
        assertEquals("Java", objectMapper.readTree(tags.getBody()).get(0).get("name").asText());
    }

    @Test
    void adminMustConfirmImpactBeforePhysicallyDeletingReferencedTag() throws Exception {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(login("admin", "admin123"));

        ResponseEntity<String> impact = restTemplate.exchange("/api/v1/tags/1/deletion-impact", HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers), String.class);
        assertEquals(200, impact.getStatusCode().value());
        assertEquals(1, objectMapper.readTree(impact.getBody()).get("referenceCount").asInt());

        ResponseEntity<String> unconfirmed = restTemplate.exchange("/api/v1/tags/1", HttpMethod.DELETE,
                new org.springframework.http.HttpEntity<>(headers), String.class);
        assertEquals(409, unconfirmed.getStatusCode().value());

        ResponseEntity<String> confirmed = restTemplate.exchange("/api/v1/tags/1?confirm=true", HttpMethod.DELETE,
                new org.springframework.http.HttpEntity<>(headers), String.class);
        assertEquals(204, confirmed.getStatusCode().value());
    }
}

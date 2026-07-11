package com.jingxuan.referencedata.web;

import com.jingxuan.BaseApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
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
}

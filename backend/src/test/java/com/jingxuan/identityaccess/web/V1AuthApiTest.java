package com.jingxuan.identityaccess.web;

import com.jingxuan.BaseApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** 使用 Testcontainers Redis 验证 refresh token 的真实轮换语义。 */
class V1AuthApiTest extends BaseApiTest {

    @Test
    void refreshTokenCanOnlyBeUsedOnce() throws Exception {
        ResponseEntity<String> login = restTemplate.exchange("/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("username", "teststu", "password", "test123"), jsonHeaders()), String.class);
        assertEquals(200, login.getStatusCode().value());
        String firstRefresh = objectMapper.readTree(login.getBody()).get("refreshToken").asText();
        assertNotNull(firstRefresh);

        ResponseEntity<String> firstRotation = restTemplate.exchange("/api/v1/auth/refresh", HttpMethod.POST,
                new HttpEntity<>(Map.of("refreshToken", firstRefresh), jsonHeaders()), String.class);
        assertEquals(200, firstRotation.getStatusCode().value());
        String secondRefresh = objectMapper.readTree(firstRotation.getBody()).get("refreshToken").asText();
        assertNotEquals(firstRefresh, secondRefresh);

        ResponseEntity<String> replay = restTemplate.exchange("/api/v1/auth/refresh", HttpMethod.POST,
                new HttpEntity<>(Map.of("refreshToken", firstRefresh), jsonHeaders()), String.class);
        assertEquals(401, replay.getStatusCode().value());
        assertEquals("UNAUTHENTICATED", objectMapper.readTree(replay.getBody()).get("code").asText());
    }
}

package com.jingxuan.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingxuan.config.DeepSeekConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * DeepSeek API 客户端 — 共享 HttpClient 和请求构建逻辑
 *
 * <p>消除 DeepSeekReviewServiceImpl 和 AiUserImportServiceImpl 中重复的
 * HttpClient 创建、请求头组装、响应 JSON 提取代码。</p>
 */
@Component
@RequiredArgsConstructor
public class DeepSeekApiClient {

    private final DeepSeekConfig config;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 向 DeepSeek API 发送 POST 请求
     *
     * @param body 请求体（会被序列化为 JSON）
     * @return 原始 HTTP 响应
     */
    public HttpResponse<String> post(Map<String, Object> body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getApiUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .timeout(Duration.ofMillis(config.getTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 从 DeepSeek 响应 JSON 中提取 choices[0].message.content
     *
     * @param responseBody 原始响应体字符串
     * @return content 字段的原始字符串值
     */
    public String extractContent(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        return root.at("/choices/0/message/content").asText("");
    }
}

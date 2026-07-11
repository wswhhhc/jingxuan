package com.jingxuan.modules.sensitive.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingxuan.common.Result;
import com.jingxuan.config.DeepSeekConfig;
import com.jingxuan.modules.sensitive.service.DeepSeekReviewService;
import com.jingxuan.modules.sensitive.service.SensitiveWordDFA;
import com.jingxuan.util.DeepSeekApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekReviewServiceImpl implements DeepSeekReviewService {

    private final DeepSeekConfig deepSeekConfig;
    private final DeepSeekApiClient deepSeekApiClient;
    private final ObjectMapper objectMapper;
    private final SensitiveWordDFA sensitiveWordDFA;

    private static final String SYSTEM_PROMPT = """
            你是一个严格的内容安全审核助手。你的任务是对用户输入的文本进行审核，判断是否包含任何违规内容。

            以下内容均属于违规，必须返回 passed=false：
            1. 辱骂攻击：任何形式的脏话、粗口、侮辱性词汇（如傻逼、尼玛、草泥马、操你妈、垃圾、废物、去死等）、人身攻击、歧视性言论
            2. 色情低俗：色情描写、色情暗示、低俗内容
            3. 暴力血腥：暴力行为、血腥场景、恐怖内容
            4. 政治敏感：违反法律法规、政治敏感内容
            5. 广告引流：垃圾广告、推广信息、联系方式
            6. 个人信息泄漏：真实姓名、手机号、学号、QQ号、微信号、身份证号等

            请以 JSON 格式返回审核结果：
            {"passed": false, "category": "违规类别", "reason": "具体原因"}
            或
            {"passed": true}

            注意：只要文本包含任何粗口或脏话，passed 必须为 false。
            """;

    private static final int MAX_RETRIES = 2;

    // ==================== 公开 API ====================

    @Override
    public ReviewResult review(String text, String scene) {
        // 空文本直接放行
        if (text == null || text.isBlank()) {
            return ReviewResult.pass();
        }

        // ── 第一阶段：DFA 快速词匹配 ──
        // 高置信度违禁词命中 → 直接拒绝（无需 AI 确认，节省 API 调用）
        if (sensitiveWordDFA.contains(text)) {
            List<SensitiveWordDFA.Hit> hits = sensitiveWordDFA.findAll(text);
            log.info("DFA 命中敏感词，直接拒绝 [scene={}]: {}", scene, hits);
            return ReviewResult.fail("sensitive_word",
                    "内容包含违禁词：" + hits.get(0).word());
        }

        // ── 第二阶段：DFA 未命中 → 交由 AI 做语义审核 ──
        return callAISync(text, scene);
    }

    @Override
    public Result<Void> testConnection() {
        String apiKey = deepSeekConfig.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return Result.fail("DeepSeek API Key 未配置");
        }
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", deepSeekConfig.getModel(),
                    "messages", List.of(
                            Map.of("role", "user", "content", "ping")
                    ),
                    "max_tokens", 5
            );

            HttpResponse<String> response = deepSeekApiClient.post(requestBody);

            if (response.statusCode() == 200) {
                return Result.ok();
            } else {
                return Result.fail("DeepSeek API 返回异常: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            return Result.fail("DeepSeek API 连接失败: " + e.getMessage());
        }
    }

    // ==================== AI 调用核心 ====================

    /**
     * 同步调用 AI 审核（带重试）
     */
    private ReviewResult callAISync(String text, String scene) {
        String apiKey = deepSeekConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DeepSeek API Key 未配置，跳过 AI 审核");
            return ReviewResult.pass();
        }

        Map<String, Object> requestBody = Map.of(
                "model", deepSeekConfig.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", text)
                ),
                "response_format", Map.of("type", "json_object")
        );

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            log.error("序列化请求体失败", e);
            return handleFallback("序列化异常");
        }

        Exception lastException = null;
        Integer lastStatusCode = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("DeepSeek API 重试第 {} 次", attempt);
                    Thread.sleep(500L * attempt);
                }

                HttpResponse<String> response = deepSeekApiClient.post(requestBody);

                if (response.statusCode() != 200) {
                    log.error("DeepSeek API 返回错误: status={}, body={}", response.statusCode(), response.body());
                    lastStatusCode = response.statusCode();
                    if (response.statusCode() >= 400 && response.statusCode() < 500) {
                        return handleApiError(response.statusCode());
                    }
                    continue;
                }

                String content = deepSeekApiClient.extractContent(response.body());
                if (content.isBlank()) {
                    log.warn("DeepSeek API 返回内容为空，body={}", response.body());
                    continue;
                }

                JsonNode result = objectMapper.readTree(content);
                boolean passed = result.path("passed").asBoolean(true);
                if (passed) {
                    return ReviewResult.pass();
                }

                return ReviewResult.fail(
                        result.path("category").asText("unknown"),
                        result.path("reason").asText("内容违规")
                );

            } catch (java.net.http.HttpTimeoutException e) {
                lastException = e;
                log.warn("DeepSeek API 超时 (attempt {}/{})", attempt + 1, MAX_RETRIES + 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return handleFallback("审核请求中断");
            } catch (Exception e) {
                lastException = e;
                log.warn("DeepSeek API 调用异常 (attempt {}/{})", attempt + 1, MAX_RETRIES + 1, e);
            }
        }

        if (lastStatusCode != null) {
            return handleApiError(lastStatusCode);
        }
        return handleFallback(lastException != null
                ? "API 调用失败（已重试" + MAX_RETRIES + "次）: " + lastException.getMessage()
                : "API 返回为空（已重试" + MAX_RETRIES + "次）");
    }

    // ==================== 兜底策略 ====================

    private ReviewResult handleApiError(int statusCode) {
        String fallback = deepSeekConfig.getFallback();
        if ("bypass".equals(fallback)) {
            log.warn("DeepSeek API 错误，兜底策略为放过，status={}", statusCode);
            return ReviewResult.pass();
        }
        return ReviewResult.fail("api_error", "内容审核服务暂时不可用");
    }

    private ReviewResult handleFallback(String message) {
        String fallback = deepSeekConfig.getFallback();
        if ("bypass".equals(fallback)) {
            log.warn("DeepSeek 调用失败，兜底策略为放过: {}", message);
            return ReviewResult.pass();
        }
        return ReviewResult.fail("service_error", message);
    }
}

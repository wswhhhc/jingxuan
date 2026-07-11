package com.jingxuan.modules.sensitive.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingxuan.config.DeepSeekConfig;
import com.jingxuan.modules.sensitive.service.DeepSeekReviewService;
import com.jingxuan.modules.sensitive.service.SensitiveWordDFA;
import com.jingxuan.util.DeepSeekApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeepSeekReviewServiceImpl - 内容审核服务")
class DeepSeekReviewServiceImplTest {

    @Mock private DeepSeekConfig deepSeekConfig;
    @Mock private ObjectMapper objectMapper;
    @Mock private SensitiveWordDFA sensitiveWordDFA;
    @Mock private DeepSeekApiClient deepSeekApiClient;

    private DeepSeekReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new DeepSeekReviewServiceImpl(deepSeekConfig, deepSeekApiClient, objectMapper, sensitiveWordDFA);
    }

    @Nested
    @DisplayName("DFA 敏感词审核")
    class DFAScreening {

        @Test
        @DisplayName("命中 DFA 敏感词直接拒绝")
        void shouldRejectWhenDFAHits() {
            // given
            when(sensitiveWordDFA.contains(anyString())).thenReturn(true);
            when(sensitiveWordDFA.findAll(anyString()))
                    .thenReturn(List.of(new SensitiveWordDFA.Hit("傻逼", 0, 2)));

            // when
            DeepSeekReviewService.ReviewResult result = reviewService.review("这个傻逼内容", "comment");

            // then
            assertFalse(result.isPassed());
            assertEquals("sensitive_word", result.getCategory());
            assertTrue(result.getReason().contains("傻逼"));
            // DFA 命中后不会调用 AI
            verify(deepSeekConfig, never()).getApiKey();
        }

        @Test
        @DisplayName("DFA 未命中时走 AI 审核")
        void shouldCallAIWhenDFAMisses() {
            when(sensitiveWordDFA.contains(anyString())).thenReturn(false);
            when(deepSeekConfig.getApiKey()).thenReturn(null); // API Key 为空则放行

            DeepSeekReviewService.ReviewResult result = reviewService.review("正常内容", "comment");

            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("空文本直接放行")
        void shouldPassOnEmptyText() {
            DeepSeekReviewService.ReviewResult result = reviewService.review("", "comment");
            assertTrue(result.isPassed());

            result = reviewService.review(null, "comment");
            assertTrue(result.isPassed());

            verifyNoInteractions(sensitiveWordDFA);
        }
    }

    @Nested
    @DisplayName("AI 审核 - API Key 为空")
    class AIReviewNoKey {

        @Test
        @DisplayName("API Key 未配置时放行")
        void shouldPassWhenNoApiKey() {
            when(sensitiveWordDFA.contains(anyString())).thenReturn(false);
            when(deepSeekConfig.getApiKey()).thenReturn(null);

            DeepSeekReviewService.ReviewResult result = reviewService.review("一些内容", "comment");

            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("API Key 为空字符串时放行")
        void shouldPassWhenApiKeyBlank() {
            when(sensitiveWordDFA.contains(anyString())).thenReturn(false);
            when(deepSeekConfig.getApiKey()).thenReturn("");

            DeepSeekReviewService.ReviewResult result = reviewService.review("一些内容", "comment");

            assertTrue(result.isPassed());
        }
    }

    @Nested
    @DisplayName("Fallback 策略")
    class FallbackStrategy {

        @Test
        @DisplayName("fallback=bypass 时 API 异常仍放行")
        void shouldBypassWhenFallbackBypass() throws Exception {
            when(sensitiveWordDFA.contains(anyString())).thenReturn(false);
            when(deepSeekConfig.getApiKey()).thenReturn("sk-test");
            when(deepSeekConfig.getModel()).thenReturn("deepseek-chat");
            when(deepSeekConfig.getFallback()).thenReturn("bypass");
            when(objectMapper.writeValueAsString(any(Map.class))).thenThrow(new RuntimeException("序列化失败"));

            DeepSeekReviewService.ReviewResult result = reviewService.review("一些内容", "comment");

            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("fallback=reject 时 API 异常返回违规")
        void shouldRejectWhenFallbackReject() throws Exception {
            when(sensitiveWordDFA.contains(anyString())).thenReturn(false);
            when(deepSeekConfig.getApiKey()).thenReturn("sk-test");
            when(deepSeekConfig.getModel()).thenReturn("deepseek-chat");
            when(deepSeekConfig.getFallback()).thenReturn("reject");
            when(objectMapper.writeValueAsString(any(Map.class))).thenThrow(new RuntimeException("序列化失败"));

            DeepSeekReviewService.ReviewResult result = reviewService.review("一些内容", "comment");

            assertFalse(result.isPassed());
        }
    }
}

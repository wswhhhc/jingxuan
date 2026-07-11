package com.jingxuan.modules.userimport.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingxuan.config.DeepSeekConfig;
import com.jingxuan.entity.SysRole;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.util.DeepSeekApiClient;
import com.jingxuan.modules.userimport.dto.AiImportMessage;
import com.jingxuan.modules.userimport.dto.AiUserImportRequest;
import com.jingxuan.modules.userimport.dto.AiUserImportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiUserImportServiceImpl - AI 批量导入用户")
class AiUserImportServiceImplTest {

    @Mock private DeepSeekConfig deepSeekConfig;
    @Mock private SysRoleMapper sysRoleMapper;
    @Mock private SysDictMapper sysDictMapper;
    @Mock private DeepSeekApiClient deepSeekApiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AiUserImportServiceImpl aiUserImportService;

    @BeforeEach
    void setUp() {
        aiUserImportService = new AiUserImportServiceImpl(deepSeekConfig, deepSeekApiClient, objectMapper, sysRoleMapper, sysDictMapper);
        // 默认 mock DeepSeek API 返回空响应（各测试按需覆盖）
        try {
            String defaultAiContent = "{\"assistantReply\":\"ok\",\"ready\":true,\"users\":[]}";
            String defaultWrapper = "{\"choices\":[{\"message\":{\"content\":" +
                    objectMapper.valueToTree(defaultAiContent).toString() + "}}]}";
            java.net.http.HttpResponse<String> defaultResp = mockResponse(200, defaultWrapper);
            doReturn(defaultAiContent).when(deepSeekApiClient).extractContent(anyString());
            when(deepSeekApiClient.post(any(java.util.Map.class)))
                    .thenReturn(defaultResp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AiUserImportRequest requestWithMessages(String... contents) {
        AiUserImportRequest req = new AiUserImportRequest();
        List<AiImportMessage> messages = java.util.Arrays.stream(contents)
                .map(c -> {
                    AiImportMessage msg = new AiImportMessage();
                    msg.setRole("user");
                    msg.setContent(c);
                    return msg;
                })
                .collect(java.util.stream.Collectors.toList());
        req.setMessages(messages);
        return req;
    }

    private SysRole studentRole() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleName("学生");
        return role;
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(int statusCode, String body) {
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(statusCode);
        when(resp.body()).thenReturn(body);
        return resp;
    }

    @SuppressWarnings("unchecked")
    private void stubDeepSeekCall(HttpResponse<String> resp) {
        try {
            // 从包装 JSON 中提取内层 AI 内容，供 extractContent mock 返回
            String body = resp.body();
            String innerContent = "";
            try {
                innerContent = objectMapper.readTree(body).at("/choices/0/message/content").asText("");
            } catch (Exception ignored) {
                innerContent = body;
            }
            doReturn(innerContent).when(deepSeekApiClient).extractContent(anyString());
            when(deepSeekApiClient.post(any(java.util.Map.class)))
                    .thenReturn(resp);
        } catch (Exception e) {
            fail("Mock setup failed");
        }
    }

    @Nested
    @DisplayName("参数校验")
    class Validation {

        @Test
        @DisplayName("空消息列表抛异常")
        void shouldThrowWhenMessagesEmpty() {
            AiUserImportRequest req = new AiUserImportRequest();
            req.setMessages(List.of());

            assertThrows(BusinessException.class,
                    () -> aiUserImportService.parse(req));
        }

        @Test
        @DisplayName("null 请求抛异常")
        void shouldThrowWhenRequestNull() {
            assertThrows(BusinessException.class,
                    () -> aiUserImportService.parse(null));
        }

        @Test
        @DisplayName("API Key 未配置抛异常")
        void shouldThrowWhenApiKeyMissing() {
            when(deepSeekConfig.getApiKey()).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> aiUserImportService.parse(requestWithMessages("创建10个学生账号")));
        }

        @Test
        @DisplayName("API Key 为空字符串抛异常")
        void shouldThrowWhenApiKeyBlank() {
            when(deepSeekConfig.getApiKey()).thenReturn("");

            assertThrows(BusinessException.class,
                    () -> aiUserImportService.parse(requestWithMessages("创建10个学生账号")));
        }
    }

    @Nested
    @DisplayName("消息过滤")
    class MessageFiltering {

        @Test
        @DisplayName("只保留 user 和 assistant 角色的消息")
        void shouldParseWithValidMessages() {
            when(deepSeekConfig.getApiKey()).thenReturn("sk-test");
            when(deepSeekConfig.getApiUrl()).thenReturn("https://api.deepseek.com/v1/chat/completions");
            when(deepSeekConfig.getModel()).thenReturn("deepseek-chat");
            when(deepSeekConfig.getTimeout()).thenReturn(30000);
            when(sysRoleMapper.selectList(null)).thenReturn(List.of(studentRole()));
            when(sysDictMapper.selectList(null)).thenReturn(List.of());

            String aiContent = "{\"assistantReply\":\"已整理好\",\"ready\":true,\"users\":[" +
                    "{\"username\":\"test01\",\"realName\":\"测试01\",\"roleName\":\"学生\"}]}";
            String wrapperJson = "{\"choices\":[{\"message\":{\"content\":" +
                    objectMapper.valueToTree(aiContent).toString() + "}}]}";

            HttpResponse<String> httpResponse = mockResponse(200, wrapperJson);
            stubDeepSeekCall(httpResponse);

            AiUserImportRequest req = requestWithMessages("创建10个学生账号");
            AiImportMessage sysMsg = new AiImportMessage();
            sysMsg.setRole("system");
            sysMsg.setContent("system message");
            req.getMessages().add(sysMsg);

            AiUserImportResponse result = aiUserImportService.parse(req);

            assertTrue(result.isReady());
            assertEquals("已整理好", result.getAssistantReply());
            assertEquals(1, result.getUsers().size());
            assertEquals("test01", result.getUsers().get(0).getUsername());
        }
    }

    @Nested
    @DisplayName("AI API 调用")
    class ApiCall {

        @Test
        @DisplayName("成功解析 AI 响应")
        void shouldParseSuccessfulResponse() {
            when(deepSeekConfig.getApiKey()).thenReturn("sk-test");
            when(deepSeekConfig.getApiUrl()).thenReturn("https://api.deepseek.com/v1/chat/completions");
            when(deepSeekConfig.getModel()).thenReturn("deepseek-chat");
            when(deepSeekConfig.getTimeout()).thenReturn(30000);
            when(sysRoleMapper.selectList(null)).thenReturn(List.of(studentRole()));
            when(sysDictMapper.selectList(null)).thenReturn(List.of());

            String aiContent = "{\"assistantReply\":\"已整理好10个账号\",\"ready\":true,\"users\":[" +
                    "{\"username\":\"user1\",\"realName\":\"用户1\",\"roleName\":\"学生\",\"password\":\"123456\"}," +
                    "{\"username\":\"user2\",\"realName\":\"用户2\",\"roleName\":\"学生\"}" +
                    "]}";
            String wrapperJson = "{\"choices\":[{\"message\":{\"content\":" +
                    objectMapper.valueToTree(aiContent).toString() + "}}]}";

            HttpResponse<String> httpResponse = mockResponse(200, wrapperJson);
            stubDeepSeekCall(httpResponse);

            AiUserImportResponse result = aiUserImportService.parse(
                    requestWithMessages("创建10个学生账号"));

            assertTrue(result.isReady());
            assertEquals(2, result.getUsers().size());
            assertEquals("user1", result.getUsers().get(0).getUsername());
            assertNotNull(result.getUsers().get(1).getPassword());
        }

        @Test
        @DisplayName("API 返回非 200 抛异常")
        void shouldThrowWhenApiError() {
            when(deepSeekConfig.getApiKey()).thenReturn("sk-test");
            when(deepSeekConfig.getApiUrl()).thenReturn("https://api.deepseek.com/v1/chat/completions");
            when(deepSeekConfig.getModel()).thenReturn("deepseek-chat");
            when(deepSeekConfig.getTimeout()).thenReturn(30000);
            when(sysRoleMapper.selectList(null)).thenReturn(List.of());
            when(sysDictMapper.selectList(null)).thenReturn(List.of());

            HttpResponse<String> httpResponse = mockResponse(429, "Too Many Requests");
            stubDeepSeekCall(httpResponse);

            assertThrows(BusinessException.class,
                    () -> aiUserImportService.parse(requestWithMessages("创建用户")));
        }

        @Test
        @DisplayName("AI 返回内容为空白抛异常")
        void shouldThrowWhenAiContentBlank() {
            when(deepSeekConfig.getApiKey()).thenReturn("sk-test");
            when(deepSeekConfig.getApiUrl()).thenReturn("https://api.deepseek.com/v1/chat/completions");
            when(deepSeekConfig.getModel()).thenReturn("deepseek-chat");
            when(deepSeekConfig.getTimeout()).thenReturn(30000);
            when(sysRoleMapper.selectList(null)).thenReturn(List.of());
            when(sysDictMapper.selectList(null)).thenReturn(List.of());

            HttpResponse<String> httpResponse = mockResponse(200, "{\"choices\":[{\"message\":{\"content\":\"\"}}]}");
            stubDeepSeekCall(httpResponse);

            assertThrows(BusinessException.class,
                    () -> aiUserImportService.parse(requestWithMessages("创建用户")));
        }

        @Test
        @DisplayName("JSON 解析失败抛异常")
        void shouldThrowWhenJsonParseFails() {
            when(deepSeekConfig.getApiKey()).thenReturn("sk-test");
            when(deepSeekConfig.getApiUrl()).thenReturn("https://api.deepseek.com/v1/chat/completions");
            when(deepSeekConfig.getModel()).thenReturn("deepseek-chat");
            when(deepSeekConfig.getTimeout()).thenReturn(30000);
            when(sysRoleMapper.selectList(null)).thenReturn(List.of());
            when(sysDictMapper.selectList(null)).thenReturn(List.of());

            HttpResponse<String> httpResponse = mockResponse(200, "{\"choices\":[{\"message\":{\"content\":\"{invalid json}\"}}]}");
            stubDeepSeekCall(httpResponse);

            assertThrows(BusinessException.class,
                    () -> aiUserImportService.parse(requestWithMessages("创建用户")));
        }
    }

    @Nested
    @DisplayName("角色与班级归一化")
    class Normalization {

        @Test
        @DisplayName("通过角色名称匹配系统角色")
        void shouldMatchRoleByName() {
            when(deepSeekConfig.getApiKey()).thenReturn("sk-test");
            when(deepSeekConfig.getApiUrl()).thenReturn("https://api.deepseek.com/v1/chat/completions");
            when(deepSeekConfig.getModel()).thenReturn("deepseek-chat");
            when(deepSeekConfig.getTimeout()).thenReturn(30000);
            when(sysRoleMapper.selectList(null)).thenReturn(List.of(studentRole()));
            when(sysDictMapper.selectList(null)).thenReturn(List.of());

            String aiContent = "{\"assistantReply\":\"OK\",\"ready\":true,\"users\":[" +
                    "{\"username\":\"user1\",\"realName\":\"用户1\",\"roleName\":\"学生\"}" +
                    "]}";
            String wrapperJson = "{\"choices\":[{\"message\":{\"content\":" +
                    objectMapper.valueToTree(aiContent).toString() + "}}]}";

            HttpResponse<String> httpResponse = mockResponse(200, wrapperJson);
            stubDeepSeekCall(httpResponse);

            AiUserImportResponse result = aiUserImportService.parse(
                    requestWithMessages("创建1个学生账号"));

            assertEquals(1, result.getUsers().size());
            assertEquals(Integer.valueOf(1), result.getUsers().get(0).getRoleId());
        }

        @Test
        @DisplayName("未知角色添加到 missingFields")
        void shouldFlagUnknownRole() {
            when(deepSeekConfig.getApiKey()).thenReturn("sk-test");
            when(deepSeekConfig.getApiUrl()).thenReturn("https://api.deepseek.com/v1/chat/completions");
            when(deepSeekConfig.getModel()).thenReturn("deepseek-chat");
            when(deepSeekConfig.getTimeout()).thenReturn(30000);
            when(sysRoleMapper.selectList(null)).thenReturn(List.of());
            when(sysDictMapper.selectList(null)).thenReturn(List.of());

            String aiContent = "{\"assistantReply\":\"OK\",\"ready\":true,\"users\":[" +
                    "{\"username\":\"user1\",\"realName\":\"用户1\",\"roleName\":\"督导员\"}" +
                    "]}";
            String wrapperJson = "{\"choices\":[{\"message\":{\"content\":" +
                    objectMapper.valueToTree(aiContent).toString() + "}}]}";

            HttpResponse<String> httpResponse = mockResponse(200, wrapperJson);
            stubDeepSeekCall(httpResponse);

            AiUserImportResponse result = aiUserImportService.parse(
                    requestWithMessages("创建1个督导员账号"));

            assertFalse(result.isReady());
            assertFalse(result.getMissingFields().isEmpty());
        }

        @Test
        @DisplayName("系统角色别名映射正确")
        void shouldMatchRoleByAlias() {
            when(deepSeekConfig.getApiKey()).thenReturn("sk-test");
            when(deepSeekConfig.getApiUrl()).thenReturn("https://api.deepseek.com/v1/chat/completions");
            when(deepSeekConfig.getModel()).thenReturn("deepseek-chat");
            when(deepSeekConfig.getTimeout()).thenReturn(30000);
            when(sysRoleMapper.selectList(null)).thenReturn(List.of(studentRole()));
            when(sysDictMapper.selectList(null)).thenReturn(List.of());

            String aiContent = "{\"assistantReply\":\"OK\",\"ready\":true,\"users\":[" +
                    "{\"username\":\"s1\",\"realName\":\"学生1\",\"roleName\":\"student\"}" +
                    "]}";
            String wrapperJson = "{\"choices\":[{\"message\":{\"content\":" +
                    objectMapper.valueToTree(aiContent).toString() + "}}]}";

            HttpResponse<String> httpResponse = mockResponse(200, wrapperJson);
            stubDeepSeekCall(httpResponse);

            AiUserImportResponse result = aiUserImportService.parse(
                    requestWithMessages("创建1个学生"));

            assertTrue(result.isReady());
            assertEquals(Integer.valueOf(1), result.getUsers().get(0).getRoleId());
        }
    }
}

package com.jingxuan.modules.userimport.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingxuan.config.DeepSeekConfig;
import com.jingxuan.util.DeepSeekApiClient;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysRole;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.modules.userimport.dto.AiImportMessage;
import com.jingxuan.modules.userimport.dto.AiImportedUserDraft;
import com.jingxuan.modules.userimport.dto.AiUserImportRequest;
import com.jingxuan.modules.userimport.dto.AiUserImportResponse;
import com.jingxuan.modules.userimport.service.AiUserImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiUserImportServiceImpl implements AiUserImportService {

    private static final List<String> REQUIRED_FIELDS = List.of("username", "realName", "role");
    private static final List<String> OPTIONAL_FIELDS = List.of("class", "password", "phone", "email", "status");

    private final DeepSeekConfig deepSeekConfig;
    private final DeepSeekApiClient deepSeekApiClient;
    private final ObjectMapper objectMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDictMapper sysDictMapper;

    @Override
    public AiUserImportResponse parse(AiUserImportRequest request) {
        List<AiImportMessage> messages = sanitizeMessages(request);
        if (messages.isEmpty()) {
            throw new BusinessException("请先输入导入需求描述");
        }

        String apiKey = deepSeekConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("DeepSeek API Key 未配置，暂时无法使用 AI 导入助手");
        }

        Map<Integer, String> roleOptions = sysRoleMapper.selectList(null).stream()
                .collect(Collectors.toMap(
                        role -> ((Number) role.getId()).intValue(),
                        SysRole::getRoleName,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        Map<Long, String> classOptions = sysDictMapper.selectList(null).stream()
                .filter(dict -> "class".equals(dict.getDictType()))
                .collect(Collectors.toMap(
                        dict -> ((Number) dict.getId()).longValue(),
                        SysDict::getDictLabel,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        String systemPrompt = buildSystemPrompt(roleOptions, classOptions);
        List<Map<String, Object>> chatMessages = new ArrayList<>();
        chatMessages.add(Map.of("role", "system", "content", systemPrompt));
        for (AiImportMessage message : messages) {
            chatMessages.add(Map.of(
                    "role", normalizeRole(message.getRole()),
                    "content", message.getContent().trim()
            ));
        }

        Map<String, Object> requestBody = Map.of(
                "model", deepSeekConfig.getModel(),
                "messages", chatMessages,
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object")
        );

        String content = callDeepSeek(requestBody);
        AiUserImportResponse response;
        try {
            response = objectMapper.readValue(content, AiUserImportResponse.class);
        } catch (Exception e) {
            log.error("AI 导入助手响应解析失败: {}", content, e);
            throw new BusinessException("AI 返回格式异常，请重试");
        }

        normalizeResponse(response, roleOptions, classOptions);
        return response;
    }

    private List<AiImportMessage> sanitizeMessages(AiUserImportRequest request) {
        if (request == null || request.getMessages() == null) {
            return List.of();
        }
        return request.getMessages().stream()
                .filter(Objects::nonNull)
                .filter(message -> message.getContent() != null && !message.getContent().isBlank())
                .filter(message -> {
                    String role = normalizeRole(message.getRole());
                    return "user".equals(role) || "assistant".equals(role);
                })
                .collect(Collectors.toList());
    }

    private String callDeepSeek(Map<String, Object> requestBody) {
        try {
            HttpResponse<String> response = deepSeekApiClient.post(requestBody);
            if (response.statusCode() != 200) {
                log.error("AI 导入助手调用失败: status={}, body={}", response.statusCode(), response.body());
                throw new BusinessException("AI 导入助手暂时不可用，请稍后重试");
            }

            String content = deepSeekApiClient.extractContent(response.body());
            if (content.isBlank()) {
                throw new BusinessException("AI 没有返回可用结果，请换种描述再试");
            }
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 导入助手调用异常", e);
            throw new BusinessException("AI 导入助手调用失败：" + e.getMessage());
        }
    }

    private String buildSystemPrompt(Map<Integer, String> roleOptions, Map<Long, String> classOptions) {
        String roleText = roleOptions.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining("，"));
        String classText = classOptions.isEmpty()
                ? "当前没有可选班级"
                : classOptions.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining("，"));

        return """
                你是“管理员账号批量导入助手”。你的目标是帮助管理员把自然语言需求整理成可导入的账号列表。

                规则：
                1. 必填字段只有 3 个：username、realName、role。
                2. 选填字段：class、password、phone、email、status。
                3. 如果信息不足以生成可导入账号，必须在 assistantReply 里主动追问最少但必要的问题，并将 ready 设为 false。
                4. 如果可以生成账号，就把 ready 设为 true，并在 users 中返回完整数组。
                5. 要能理解并展开区间/批量表达，例如“user1到5”“前10个账号”“姓名张三到张七”。
                6. 如果用户没有给 password，默认 password 为 123456，并在 assumptions 中说明。
                7. 如果用户没有给 status，默认 status 为 1，并在 assumptions 中说明。
                8. role 请优先使用以下系统角色：%s。
                9. class 如能匹配则使用以下系统班级：%s。
                10. 如果用户说的角色或班级不在系统选项里，不要编造，应该在 missingFields 和 assistantReply 中提醒管理员确认。
                11. 只返回 JSON，不要输出 Markdown。

                返回 JSON 结构：
                {
                  "assistantReply": "给管理员看的简短回复或追问",
                  "ready": true,
                  "requiredFields": ["username", "realName", "role"],
                  "optionalFields": ["class", "password", "phone", "email", "status"],
                  "missingFields": ["缺失项或待确认项"],
                  "assumptions": ["自动补全或默认值说明"],
                  "users": [
                    {
                      "username": "user1",
                      "realName": "用户1",
                      "roleName": "学生",
                      "className": "软件工程1班",
                      "password": "123456",
                      "phone": "",
                      "email": "",
                      "status": 1
                    }
                  ]
                }

                如果 ready=false，users 返回空数组也可以。
                """.formatted(roleText, classText);
    }

    private void normalizeResponse(AiUserImportResponse response,
                                   Map<Integer, String> roleOptions,
                                   Map<Long, String> classOptions) {
        if (response.getRequiredFields() == null || response.getRequiredFields().isEmpty()) {
            response.setRequiredFields(new ArrayList<>(REQUIRED_FIELDS));
        }
        if (response.getOptionalFields() == null || response.getOptionalFields().isEmpty()) {
            response.setOptionalFields(new ArrayList<>(OPTIONAL_FIELDS));
        }
        if (response.getMissingFields() == null) {
            response.setMissingFields(new ArrayList<>());
        }
        if (response.getAssumptions() == null) {
            response.setAssumptions(new ArrayList<>());
        }
        if (response.getUsers() == null) {
            response.setUsers(new ArrayList<>());
        }

        List<String> issues = new ArrayList<>(response.getMissingFields());
        for (AiImportedUserDraft user : response.getUsers()) {
            normalizeDraft(user, roleOptions, classOptions, issues, response.getAssumptions());
        }

        response.setMissingFields(issues.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .distinct()
                .collect(Collectors.toList()));

        if (response.getAssistantReply() == null || response.getAssistantReply().isBlank()) {
            response.setAssistantReply(response.isReady()
                    ? "我已经整理好了可导入账号，你可以先检查预览再导入。"
                    : "还差一些必要信息，我已经帮你标出来了。");
        }

        if (!response.getMissingFields().isEmpty()) {
            response.setReady(false);
        }
        if (response.getUsers().isEmpty()) {
            response.setReady(false);
        }
    }

    private void normalizeDraft(AiImportedUserDraft user,
                                Map<Integer, String> roleOptions,
                                Map<Long, String> classOptions,
                                List<String> issues,
                                List<String> assumptions) {
        if (user == null) {
            issues.add("存在空白用户记录，请重新生成");
            return;
        }

        user.setUsername(trimToNull(user.getUsername()));
        user.setRealName(trimToNull(user.getRealName()));
        user.setRoleName(trimToNull(user.getRoleName()));
        user.setClassName(trimToNull(user.getClassName()));
        user.setPhone(trimToNull(user.getPhone()));
        user.setEmail(trimToNull(user.getEmail()));
        user.setPassword(trimToNull(user.getPassword()));

        if (user.getUsername() == null) {
            issues.add("存在缺少用户名的账号记录");
        }
        if (user.getRealName() == null) {
            issues.add("存在缺少真实姓名的账号记录");
        }

        resolveRole(user, roleOptions, issues);
        resolveClass(user, classOptions, issues);

        if (user.getPassword() == null) {
            user.setPassword("123456");
            assumptions.add("未提供密码的账号已默认设置为 123456");
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
            assumptions.add("未提供状态的账号已默认设置为启用");
        }
    }

    private void resolveRole(AiImportedUserDraft user,
                             Map<Integer, String> roleOptions,
                             List<String> issues) {
        if (user.getRoleId() != null && roleOptions.containsKey(user.getRoleId())) {
            user.setRoleName(roleOptions.get(user.getRoleId()));
            return;
        }
        if (user.getRoleName() == null) {
            issues.add("账号「" + safeName(user.getUsername()) + "」缺少角色");
            return;
        }

        String normalizedRole = normalizeOptionText(user.getRoleName());
        for (Map.Entry<Integer, String> entry : roleOptions.entrySet()) {
            if (normalizeOptionText(entry.getValue()).equals(normalizedRole)) {
                user.setRoleId(entry.getKey());
                user.setRoleName(entry.getValue());
                return;
            }
        }

        Map<String, Integer> aliases = Map.of(
                "student", 1,
                "teacher", 2,
                "admin", 3,
                "学生", 1,
                "教师", 2,
                "管理员", 3
        );
        Integer matchedRoleId = aliases.get(normalizedRole);
        if (matchedRoleId != null && roleOptions.containsKey(matchedRoleId)) {
            user.setRoleId(matchedRoleId);
            user.setRoleName(roleOptions.get(matchedRoleId));
            return;
        }

        issues.add("账号「" + safeName(user.getUsername()) + "」的角色「" + user.getRoleName() + "」不在系统可选项中");
    }

    private void resolveClass(AiImportedUserDraft user,
                              Map<Long, String> classOptions,
                              List<String> issues) {
        if (user.getClassId() != null && classOptions.containsKey(user.getClassId())) {
            user.setClassName(classOptions.get(user.getClassId()));
            return;
        }
        if (user.getClassName() == null) {
            return;
        }
        String normalizedClass = normalizeOptionText(user.getClassName());
        for (Map.Entry<Long, String> entry : classOptions.entrySet()) {
            if (normalizeOptionText(entry.getValue()).equals(normalizedClass)) {
                user.setClassId(entry.getKey());
                user.setClassName(entry.getValue());
                return;
            }
        }
        issues.add("账号「" + safeName(user.getUsername()) + "」的班级「" + user.getClassName() + "」不在系统可选项中");
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "user";
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        return Arrays.asList("assistant", "system").contains(normalized) ? normalized : "user";
    }

    private String normalizeOptionText(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeName(String username) {
        return username == null || username.isBlank() ? "未命名账号" : username;
    }
}

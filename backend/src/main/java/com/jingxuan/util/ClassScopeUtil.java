package com.jingxuan.util;

import cn.hutool.json.JSONUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 班级范围解析工具类
 *
 * <p>消除 ScoreBatchServiceImpl / TeacherWorkFacade / ScoreServiceImpl
 * 中重复的 JSON 数组 + 旧格式逗号分隔 fallback 解析逻辑。</p>
 */
public class ClassScopeUtil {

    private ClassScopeUtil() {}

    /**
     * 将班级范围字符串解析为 {@code Set<String>}
     * <p>支持 JSON 数组格式 {@code ["1","2","3"]} 和旧版逗号分隔格式 {@code 1,2,3}。</p>
     *
     * @param classScopes 班级范围字符串，可能为 null 或空白
     * @return 解析后的值集合，不会为 null
     */
    public static Set<String> parseToStringSet(String classScopes) {
        if (classScopes == null || classScopes.isBlank()) {
            return Collections.emptySet();
        }
        String trimmed = classScopes.trim();
        try {
            return new HashSet<>(JSONUtil.parseArray(trimmed).toList(String.class));
        } catch (Exception e) {
            // 兼容旧数据中的逗号分隔格式（中英文逗号均可）
            String[] parts = trimmed
                    .replace("[", "").replace("]", "").replace("\"", "")
                    .split("[,，]");
            return Arrays.stream(parts)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
    }
}

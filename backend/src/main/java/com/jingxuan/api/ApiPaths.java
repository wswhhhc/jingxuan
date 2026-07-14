package com.jingxuan.api;

import java.util.Locale;
import java.util.Set;

/** API 路径契约判断。 */
public final class ApiPaths {

    private static final String V1_ROOT = "/api/v1";
    private static final String SHOWCASE_WORKS = "/api/v1/showcase/works";
    private static final Set<String> PUBLIC_GET_PATHS = Set.of(
            "/api/v1/classes",
            "/api/v1/tags",
            "/api/v1/leaderboards",
            "/api/v1/leaderboards/categories",
            "/api/v1/notices/published");
    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/challenges",
            "/api/v1/auth/email-verifications",
            "/api/v1/auth/registrations");
    private static final Set<String> STORAGE_BACKED_RATE_LIMITED_POST_PATHS = Set.of(
            "/api/v1/auth/challenges",
            "/api/v1/auth/email-verifications",
            "/api/v1/users/batch/ai-parse");

    private ApiPaths() {
    }

    public static boolean isV1(String path) {
        return V1_ROOT.equals(path) || (path != null && path.startsWith(V1_ROOT + "/"));
    }

    /** 与 SecurityConfig 及 OpenAPI 共用的 V1 匿名访问边界。 */
    public static boolean isPublicV1Operation(String method, String path) {
        String normalizedMethod = normalizeMethod(method);
        if ("POST".equals(normalizedMethod)) {
            return PUBLIC_POST_PATHS.contains(path);
        }
        if (!"GET".equals(normalizedMethod)) {
            return false;
        }
        return PUBLIC_GET_PATHS.contains(path)
                || pathWithin(path, "/api/v1/dictionaries")
                || isShowcaseV1Operation(normalizedMethod, path)
                || singleSegmentBelow(path, "/api/v1/notices/");
    }

    /** 运行时可能返回 429 的 V1 操作，包括入口保护和公开展示限流。 */
    public static boolean isRateLimitedV1Operation(String method, String path) {
        String normalizedMethod = normalizeMethod(method);
        return "POST".equals(normalizedMethod) && STORAGE_BACKED_RATE_LIMITED_POST_PATHS.contains(path)
                || isShowcaseV1Operation(normalizedMethod, path);
    }

    /** 使用 Redis 限流存储、因此可能返回 503 的 V1 操作。 */
    public static boolean isRateLimitStorageBackedV1Operation(String method, String path) {
        return "POST".equals(normalizeMethod(method))
                && STORAGE_BACKED_RATE_LIMITED_POST_PATHS.contains(path);
    }

    /** PublicRateLimitFilter 管理的公开展示读取路径。 */
    public static boolean isShowcaseV1Operation(String method, String path) {
        return "GET".equals(normalizeMethod(method)) && pathWithin(path, SHOWCASE_WORKS);
    }

    private static String normalizeMethod(String method) {
        return method == null ? "" : method.toUpperCase(Locale.ROOT);
    }

    private static boolean pathWithin(String path, String root) {
        return root.equals(path) || path != null && path.startsWith(root + "/");
    }

    private static boolean singleSegmentBelow(String path, String prefix) {
        if (path == null || !path.startsWith(prefix) || path.length() == prefix.length()) {
            return false;
        }
        return path.indexOf('/', prefix.length()) < 0;
    }
}

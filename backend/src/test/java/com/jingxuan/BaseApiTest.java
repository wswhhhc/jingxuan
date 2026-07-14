package com.jingxuan;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.jingxuan.common.Result;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.lifecycle.Startables;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API 集成测试基类
 *
 * <p>启动完整 Spring 上下文，并通过 Testcontainers 自动提供 MySQL 8 与 Redis 7。
 * 测试不依赖开发机上的数据库、Redis 或 DB_PASSWORD。每个测试方法前自动登录
 * 获取对应角色的 JWT Token。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * class MyTest extends BaseApiTest {
 *     @Test
 *     void testSomething() {
 *         ApiResponse resp = adminApi.get("/admin/xxx");
 *         resp.assertOk();
 *         String title = resp.getData("title", String.class);
 *     }
 * }
 * }</pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Tag("integration")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/cleanup.sql",
        "classpath:sql/test-data.sql"
})
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS, scripts = {
        "classpath:sql/cleanup.sql"
})
public abstract class BaseApiTest {

    private static final Path TEST_TARGET_DIR = Paths.get("target").toAbsolutePath().normalize();
    private static final Path TEST_UPLOAD_DIR = TEST_TARGET_DIR.resolve("test-uploads").normalize();

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(TestContainerImages.mysql())
            .withDatabaseName("jingxuan_test")
            .withUsername("jingxuan_test")
            .withPassword("jingxuan_test");

    private static final GenericContainer<?> REDIS = new GenericContainer<>(TestContainerImages.redis())
            .withExposedPorts(6379);

    static {
        Startables.deepStart(Stream.of(MYSQL, REDIS)).join();
    }

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("jingxuan.upload.path", () -> TEST_UPLOAD_DIR.toString());
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /** 管理端 API 调用器（已带 admin token） */
    protected ApiClient adminApi;
    /** 教师端 API 调用器（已带 teacher token） */
    protected ApiClient teacherApi;
    /** 学生端 API 调用器（2022001/张三，已有作品） */
    protected ApiClient studentApi;
    /** 测试学生 API 调用器（teststu，无作品，适合 CRUD 测试） */
    protected ApiClient testStuApi;
    /** 公开端 API 调用器（无 token） */
    protected ApiClient publicApi;

    @BeforeEach
    void setUp() {
        cleanTestUploadDirectory();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });

        String baseUrl = "http://localhost:" + port;
        adminApi = new ApiClient(baseUrl, login("admin", "admin123"));
        teacherApi = new ApiClient(baseUrl, login("t001", "test123"));
        studentApi = new ApiClient(baseUrl, login("2022001", "test123"));
        testStuApi = new ApiClient(baseUrl, login("teststu", "test123"));
        publicApi = new ApiClient(baseUrl, null);
    }

    @AfterAll
    static void cleanUpTestFiles() {
        cleanTestUploadDirectory();
    }

    private static void cleanTestUploadDirectory() {
        if (!TEST_UPLOAD_DIR.startsWith(TEST_TARGET_DIR)) {
            throw new IllegalStateException("拒绝清理 target 目录之外的测试上传路径: " + TEST_UPLOAD_DIR);
        }
        if (!Files.exists(TEST_UPLOAD_DIR)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(TEST_UPLOAD_DIR)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException | UncheckedIOException e) {
            throw new IllegalStateException("清理测试上传目录失败: " + TEST_UPLOAD_DIR, e);
        }
    }

    /** 通过登录 API 获取 JWT Token */
    protected String login(String username, String password) {
        ResponseEntity<String> raw = restTemplate.exchange(
                "/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("username", username, "password", password), jsonHeaders()),
                String.class);
        String body = raw.getBody();
        assertEquals(200, raw.getStatusCode().value(), "登录失败 [" + username + "]: " + body);
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode dataNode = root.get("data");
            // AuthController 返回 Result.ok("登录成功", response) 即 code=200, message="登录成功", data=LoginResponse
            assertNotNull(dataNode, "登录返回 data 为 null，body: " + body);
            JsonNode tokenNode = dataNode.get("token");
            assertNotNull(tokenNode, "登录未返回 token 字段，data: " + dataNode + "，body: " + body);
            String token = tokenNode.asText();
            assertFalse(token.isEmpty(), "登录返回空 token");
            return token;
        } catch (Exception e) {
            throw new RuntimeException("解析登录响应失败: " + body, e);
        }
    }

    // ==================== HTTP 工具 ====================

    protected HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /** 发送请求并返回原始 JSON 字符串 */
    protected String request(HttpMethod method, String url, Object body, String token) {
        HttpHeaders headers = jsonHeaders();
        if (token != null) headers.setBearerAuth(token);
        ResponseEntity<String> resp = restTemplate.exchange(
                url, method, new HttpEntity<>(body, headers), String.class);
        return resp.getBody();
    }

    // ==================== 响应封装 ====================

    /** 简化的 API 响应封装，方便子类使用 */
    public class ApiResponse {
        private final JsonNode root;

        public ApiResponse(String json) {
            try {
                this.root = objectMapper.readTree(json);
            } catch (Exception e) {
                throw new RuntimeException("JSON 解析失败: " + json, e);
            }
        }

        public int getCode() {
            return root.get("code").asInt();
        }

        public String getMessage() {
            JsonNode m = root.get("message");
            return m != null ? m.asText() : null;
        }

        public JsonNode getDataNode() {
            return root.get("data");
        }

        public <T> T getData(Class<T> type) {
            JsonNode data = getDataNode();
            if (data == null || data.isNull()) return null;
            return objectMapper.convertValue(data, type);
        }

        public <T> T getData(TypeReference<T> typeRef) {
            JsonNode data = getDataNode();
            if (data == null || data.isNull()) return null;
            return objectMapper.convertValue(data, typeRef);
        }

        /** 断言 code=200 */
        public ApiResponse assertOk() {
            assertEquals(200, getCode(), "业务错误: " + getMessage());
            return this;
        }

        /** 断言指定业务 code */
        public ApiResponse assertCode(int expectedCode) {
            assertEquals(expectedCode, getCode());
            return this;
        }

        /** 取嵌套在 data 内的字段值，如 data.userInfo.roleCode */
        public String getDataNested(String field1, String field2) {
            JsonNode data = getDataNode();
            if (data == null) return null;
            JsonNode f1 = data.get(field1);
            if (f1 == null) return null;
            JsonNode f2 = f1.get(field2);
            return f2 != null ? f2.asText() : null;
        }

        /** 取 data 内某个字段的文本值 */
        public String getDataText(String field) {
            JsonNode data = getDataNode();
            if (data == null) return null;
            JsonNode f = data.get(field);
            if (f == null || f.isNull()) {
                return null;
            }
            return f.isValueNode() ? f.asText() : f.toString();
        }

        /** 取 data 内某个字段的整数值 */
        public int getDataInt(String field) {
            JsonNode data = getDataNode();
            if (data == null) return 0;
            JsonNode f = data.get(field);
            return f != null ? f.asInt() : 0;
        }

        /** 取 data 内某个字段的布尔值 */
        public boolean getDataBool(String field) {
            JsonNode data = getDataNode();
            if (data == null) return false;
            JsonNode f = data.get(field);
            return f != null && f.asBoolean();
        }
    }

    // ==================== API 调用器 ====================

    public class ApiClient {
        private final String baseUrl;
        private final String token;

        public ApiClient(String baseUrl, String token) {
            this.baseUrl = baseUrl;
            this.token = token;
        }

        public ApiResponse get(String path) {
            return new ApiResponse(request(HttpMethod.GET, baseUrl + path, null, token));
        }

        public ApiResponse get(String path, Map<String, ?> params) {
            String qs = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + "&" + b)
                    .map(s -> "?" + s)
                    .orElse("");
            return new ApiResponse(request(HttpMethod.GET, baseUrl + path + qs, null, token));
        }

        public ApiResponse post(String path, Object body) {
            return new ApiResponse(request(HttpMethod.POST, baseUrl + path, body, token));
        }

        public ApiResponse put(String path, Object body) {
            return new ApiResponse(request(HttpMethod.PUT, baseUrl + path, body, token));
        }

        public ApiResponse delete(String path) {
            return new ApiResponse(request(HttpMethod.DELETE, baseUrl + path, null, token));
        }
    }
}


package com.jingxuan.config;

import com.fasterxml.jackson.databind.JavaType;
import com.jingxuan.api.ApiPaths;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Springdoc OpenAPI 配置
 */
@Configuration
public class OpenApiConfig {

    private static final String PROBLEM_DETAILS_REF = "#/components/schemas/ProblemDetails";
    private static final Map<String, String> STANDARD_PROBLEM_RESPONSES = standardProblemResponses();
    private static final List<String> COMMON_PROBLEM_RESPONSES = List.of(
            "400", "404", "405", "406", "422", "500");

    @Bean
    public OpenAPI customOpenAPI() {
        Components components = new Components()
                .addSecuritySchemes("BearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("输入 Bearer Token，格式: Bearer {token}"));
        return new OpenAPI()
                .info(new Info()
                        .title("学院作品展示平台 API 文档")
                        .version("1.0.0")
                        .description("学院作品展示平台后端接口文档")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(components);
    }

    /**
     * Jackson 会把 boxed {@link Long} 序列化为 JSON 字符串；OpenAPI 必须使用相同类型，
     * 否则 JavaScript 客户端会把雪花 ID 生成为不安全的 number。
     */
    @Bean
    public ModelConverter boxedLongAsStringModelConverter() {
        return (type, context, chain) -> {
            Schema<?> schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
            if (schema != null && type != null && isBoxedLong(type.getType())) {
                schema.setType("string");
                schema.setTypes(Set.of("string"));
                schema.setFormat(null);
            }
            return schema;
        };
    }

    /** 按公开、认证、请求体和限流边界生成与运行时一致的 V1 契约。 */
    @Bean
    public OpenApiCustomizer v1ContractOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSchemas("ProblemDetails", problemDetailsSchema());
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> {
                if (!ApiPaths.isV1(path)) {
                    return;
                }
                pathItem.readOperationsMap().forEach((method, operation) ->
                        customizeV1Operation(method, path, operation));
            });
        };
    }

    private static void customizeV1Operation(PathItem.HttpMethod method, String path, Operation operation) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        ApiResponses responses = operation.getResponses();
        responses.keySet().stream()
                .filter(OpenApiConfig::isErrorStatus)
                .toList()
                .forEach(status -> putProblemResponse(responses, status));
        COMMON_PROBLEM_RESPONSES.forEach(status -> putProblemResponse(responses, status));

        String methodName = method.name();
        if (ApiPaths.isPublicV1Operation(methodName, path)) {
            operation.setSecurity(Collections.emptyList());
        } else {
            putProblemResponse(responses, "401");
            putProblemResponse(responses, "403");
        }

        if (operation.getRequestBody() == null) {
            responses.remove("415");
        } else {
            putProblemResponse(responses, "415");
        }

        if (ApiPaths.isRateLimitedV1Operation(methodName, path)) {
            putProblemResponse(responses, "429");
        } else {
            responses.remove("429");
        }

        if (ApiPaths.isRateLimitStorageBackedV1Operation(methodName, path)) {
            putProblemResponse(responses, "503");
        }
    }

    private static void putProblemResponse(ApiResponses responses, String status) {
        ApiResponse existing = responses.get(status);
        String description = existing != null && existing.getDescription() != null
                ? existing.getDescription() : STANDARD_PROBLEM_RESPONSES.get(status);
        responses.addApiResponse(status, problemResponse(
                description == null ? "请求失败" : description));
    }

    private static boolean isErrorStatus(String status) {
        return status != null && status.matches("[45]\\d{2}");
    }

    private static ApiResponse problemResponse(String description) {
        Schema<?> schema = new Schema<>().$ref(PROBLEM_DETAILS_REF);
        io.swagger.v3.oas.models.media.MediaType mediaType =
                new io.swagger.v3.oas.models.media.MediaType().schema(schema);
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE, mediaType));
    }

    private static Schema<?> problemDetailsSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.setDescription("统一 API 错误响应");
        schema.addProperty("type", new StringSchema().description("问题类型 URI"));
        schema.addProperty("title", new StringSchema().description("问题标题"));
        schema.addProperty("status", new IntegerSchema().format("int32").description("HTTP 状态码"));
        schema.addProperty("detail", new StringSchema().description("面向用户的错误详情"));
        schema.addProperty("instance", new StringSchema().description("请求实例 URI"));
        schema.addProperty("code", new StringSchema().description("机器可读业务错误码"));
        schema.addProperty("requestId", new StringSchema().description("请求追踪 ID"));
        schema.addProperty("fieldErrors",
                new MapSchema().additionalProperties(new StringSchema())
                        .description("字段校验错误（键为字段名，值为错误消息）"));
        schema.setRequired(List.of(
                "type", "title", "status", "detail", "instance", "code", "requestId", "fieldErrors"));
        return schema;
    }

    private static boolean isBoxedLong(Type type) {
        return Long.class.equals(type)
                || type instanceof JavaType javaType && Long.class.equals(javaType.getRawClass());
    }

    private static Map<String, String> standardProblemResponses() {
        Map<String, String> responses = new LinkedHashMap<>();
        responses.put("400", "请求格式或参数无效");
        responses.put("401", "未提供有效身份凭证");
        responses.put("403", "当前身份无权执行该操作");
        responses.put("404", "资源或路由不存在");
        responses.put("405", "请求方法不受支持");
        responses.put("406", "无法生成客户端可接受的响应格式");
        responses.put("415", "请求媒体类型不受支持");
        responses.put("422", "请求内容未通过业务校验");
        responses.put("429", "请求过于频繁");
        responses.put("500", "服务器内部错误");
        responses.put("503", "服务暂时不可用");
        return Collections.unmodifiableMap(responses);
    }
}

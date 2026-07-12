package com.jingxuan.config;

import com.jingxuan.identityaccess.api.V1ChallengeResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.core.util.Json31;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    @Test
    void exposesOnlyVersionedApiContractGroup() {
        GroupedOpenApi v1OpenApi = new OpenApiConfig().v1OpenApi();

        assertEquals("v1", v1OpenApi.getGroup());
        assertEquals(List.of("/api/v1/**"), v1OpenApi.getPathsToMatch());
    }

    @Test
    void customizesSecurityAndProblemResponsesPerOperation() {
        OpenAPI openApi = fixture();

        customize(openApi);

        Operation login = openApi.getPaths().get("/api/v1/auth/login").getPost();
        Operation challenge = openApi.getPaths().get("/api/v1/auth/challenges").getPost();
        Operation classes = openApi.getPaths().get("/api/v1/classes").getGet();
        Operation protectedGet = openApi.getPaths().get("/api/v1/me/works").getGet();
        Operation protectedPost = openApi.getPaths().get("/api/v1/me/works").getPost();

        assertTrue(login.getSecurity().isEmpty());
        assertTrue(challenge.getSecurity().isEmpty());
        assertTrue(classes.getSecurity().isEmpty());
        assertProblemResponse(login, "401");
        assertProblemResponse(challenge, "422");
        assertTrue(protectedGet.getSecurity().stream()
                .anyMatch(requirement -> requirement.containsKey("BearerAuth")));
        assertProblemResponse(protectedGet, "401");
        assertProblemResponse(protectedGet, "403");
        assertProblemResponse(protectedPost, "422");
        assertProblemResponse(protectedPost, "409");
        assertEquals("版本冲突", protectedPost.getResponses().get("409").getDescription());
        assertProblemResponse(protectedGet, "418");
        assertEquals("旧泛型错误", protectedGet.getResponses().get("418").getDescription());
        Schema<?> problemDetails = openApi.getComponents().getSchemas().get("ProblemDetails");
        assertNotNull(problemDetails);
        assertTrue("object".equals(problemDetails.getType())
                || (problemDetails.getTypes() != null && problemDetails.getTypes().contains("object")));
    }

    @Test
    void normalizesSnowflakeIdentifiersAndBuildServer() {
        OpenAPI openApi = fixture();

        customize(openApi);

        assertEquals(List.of("/"), openApi.getServers().stream().map(Server::getUrl).toList());
        assertNull(openApi.getSecurity());
        assertStringIdentifier(openApi, "ScoreSubmitRequest", "workId");
        assertStringIdentifier(openApi, "WorkMemberDTO", "id");
        assertStringIdentifier(openApi, "WorkMemberDTO", "studentId");
        assertStringIdentifier(openApi, "V1ChallengeResponse", "id");
        assertArrayIdentifier(openApi, "V1CreateWorkRequest", "attachmentIds");
        Schema<?> challengeResponse = openApi.getComponents().getSchemas().get("V1ChallengeResponse");
        assertFalse(challengeResponse.getProperties().containsKey("answer"));
        assertFalse(challengeResponse.getProperties().containsKey("challengeId"));
        Operation protectedGet = openApi.getPaths().get("/api/v1/me/works").getGet();
        Operation protectedPost = openApi.getPaths().get("/api/v1/me/works").getPost();
        assertEquals(Set.of("string"), protectedGet.getParameters().get(0).getSchema().getTypes());
        assertEquals(Set.of("string"), property(protectedPost.getRequestBody().getContent()
                .get("application/json").getSchema(), "batchId").getTypes());
        assertEquals(Set.of("string"), property(protectedGet.getResponses().get("200").getContent()
                .get("application/json").getSchema(), "ownerId").getTypes());
    }

    @Test
    void serializesProblemDetailsAsAnObjectSchemaInOpenApi31() {
        OpenAPI openApi = fixture();

        customize(openApi);

        assertEquals("object", Json31.mapper().valueToTree(openApi)
                .path("components")
                .path("schemas")
                .path("ProblemDetails")
                .path("type")
                .asText());
    }

    private void customize(OpenAPI openApi) {
        GroupedOpenApi groupedOpenApi = new OpenApiConfig().v1OpenApi();
        assertEquals(1, groupedOpenApi.getOpenApiCustomizers().size());
        groupedOpenApi.getOpenApiCustomizers().forEach(customizer -> customizer.customise(openApi));
    }

    private OpenAPI fixture() {
        ObjectSchema scoreRequest = new ObjectSchema();
        scoreRequest.addProperty("workId", new IntegerSchema().format("int64"));
        ObjectSchema member = new ObjectSchema();
        member.addProperty("id", new IntegerSchema().format("int64"));
        member.addProperty("studentId", new IntegerSchema().format("int64"));
        ObjectSchema createWorkRequest = new ObjectSchema();
        Schema<Object> attachmentIds = new Schema<>();
        attachmentIds.setType("array");
        attachmentIds.setItems(new IntegerSchema().format("int64"));
        createWorkRequest.addProperty("attachmentIds", attachmentIds);
        Components components = new Components()
                .addSchemas("ScoreSubmitRequest", scoreRequest)
                .addSchemas("WorkMemberDTO", member)
                .addSchemas("V1CreateWorkRequest", createWorkRequest);
        ModelConverters.getInstance().readAll(V1ChallengeResponse.class)
                .forEach(components::addSchemas);

        Operation login = operation(false, true);
        Operation challenge = operation(false, true);
        Operation classes = operation(false, false);
        Operation protectedGet = operation(true, false);
        protectedGet.addParametersItem(new Parameter().name("id")
                .in("query").schema(new IntegerSchema().format("int64")));
        Operation protectedPost = operation(true, true);
        protectedPost.getResponses().addApiResponse("409", new ApiResponse()
                .description("版本冲突")
                .content(new Content().addMediaType("application/json",
                        new io.swagger.v3.oas.models.media.MediaType()
                                .schema(new ObjectSchema()))));
        Paths paths = new Paths()
                .addPathItem("/api/v1/auth/login", new PathItem().post(login))
                .addPathItem("/api/v1/auth/challenges", new PathItem().post(challenge))
                .addPathItem("/api/v1/classes", new PathItem().get(classes))
                .addPathItem("/api/v1/me/works", new PathItem()
                        .get(protectedGet)
                        .post(protectedPost));

        return new OpenAPI()
                .components(components)
                .paths(paths)
                .servers(List.of(new Server().url("http://localhost:18080")))
                .security(List.of(new SecurityRequirement().addList("BearerAuth")));
    }

    private Operation operation(boolean protectedOperation, boolean requestBody) {
        ObjectSchema responseSchema = new ObjectSchema();
        responseSchema.addProperty("ownerId", new IntegerSchema().format("int64"));
        Operation operation = new Operation().responses(new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("成功")
                        .content(new Content().addMediaType("application/json",
                                new io.swagger.v3.oas.models.media.MediaType().schema(responseSchema))))
                .addApiResponse("418", new ApiResponse().description("旧泛型错误")));
        if (protectedOperation) {
            operation.security(List.of(new SecurityRequirement().addList("LegacyAuth")));
        }
        if (requestBody) {
            ObjectSchema requestSchema = new ObjectSchema();
            requestSchema.addProperty("batchId", new IntegerSchema().format("int64"));
            operation.requestBody(new RequestBody().content(new Content()
                    .addMediaType("application/json",
                            new io.swagger.v3.oas.models.media.MediaType().schema(requestSchema))));
        }
        return operation;
    }

    private void assertProblemResponse(Operation operation, String status) {
        ApiResponse response = operation.getResponses().get(status);
        assertNotNull(response, "缺少 " + status + " 响应");
        assertNotNull(response.getContent().get("application/problem+json"));
        assertEquals("#/components/schemas/ProblemDetails",
                response.getContent().get("application/problem+json").getSchema().get$ref());
    }

    private void assertStringIdentifier(OpenAPI openApi, String schemaName, String propertyName) {
        Schema<?> schema = property(openApi.getComponents().getSchemas().get(schemaName), propertyName);
        assertEquals(Set.of("string"), schema.getTypes());
        assertNull(schema.getFormat());
    }

    private void assertArrayIdentifier(OpenAPI openApi, String schemaName, String propertyName) {
        Schema<?> schema = property(openApi.getComponents().getSchemas().get(schemaName), propertyName);
        assertTrue("array".equals(schema.getType())
                || (schema.getTypes() != null && schema.getTypes().contains("array")));
        assertNotNull(schema.getItems());
        assertEquals(Set.of("string"), schema.getItems().getTypes());
        assertNull(schema.getItems().getFormat());
    }

    private Schema<?> property(Schema<?> schema, String propertyName) {
        return (Schema<?>) schema.getProperties().get(propertyName);
    }
}

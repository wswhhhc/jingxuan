package com.jingxuan.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    @Test
    void exposesDefaultContract() {
        OpenAPI openApi = new OpenApiConfig().customOpenAPI();
        assertNotNull(openApi.getInfo());
        assertEquals("学院作品展示平台 API 文档", openApi.getInfo().getTitle());
        assertNotNull(openApi.getComponents().getSecuritySchemes().get("BearerAuth"));
    }
}

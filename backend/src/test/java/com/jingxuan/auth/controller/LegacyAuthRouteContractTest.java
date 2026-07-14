package com.jingxuan.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyAuthRouteContractTest {

    @Test
    void exposesBothDirectAndProxyAuthRoutes() {
        assertDirectAndProxyRoutes(AuthController.class);
        assertDirectAndProxyRoutes(RegistrationController.class);
    }

    private void assertDirectAndProxyRoutes(Class<?> controller) {
        RequestMapping mapping = controller.getAnnotation(RequestMapping.class);
        assertTrue(mapping != null, () -> controller.getSimpleName() + " 必须声明认证路由");
        assertTrue(Arrays.asList(mapping.value()).contains("/auth"),
                () -> controller.getSimpleName() + " 必须兼容直连 /auth 路径");
        assertTrue(Arrays.asList(mapping.value()).contains("/api/auth"),
                () -> controller.getSimpleName() + " 必须保留经代理的 /api/auth 路径");
    }
}

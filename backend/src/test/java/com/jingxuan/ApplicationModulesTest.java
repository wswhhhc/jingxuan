package com.jingxuan;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationModulesTest {

    @Test
    void shouldDiscoverCurrentModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        assertFalse(modules.stream().toList().isEmpty(), "阶段 1 至少应能发现当前模块结构");
        assertTrue(modules.getModuleByName("identityaccess").isPresent());
        assertTrue(modules.getModuleByName("referencedata").isPresent());
        assertTrue(modules.getModuleByName("campaign").isPresent());
        assertTrue(modules.getModuleByName("portfolio").isPresent());
        assertTrue(modules.getModuleByName("evaluation").isPresent());
        assertTrue(modules.getModuleByName("communication").isPresent());
        assertTrue(modules.getModuleByName("moderation").isPresent());
        assertTrue(modules.getModuleByName("operationsreporting").isPresent());
        assertTrue(modules.getModuleByName("workflow").isPresent());
    }
}

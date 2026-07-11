package com.jingxuan;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ApplicationModulesTest {

    @Test
    void shouldDiscoverCurrentModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        assertFalse(modules.stream().toList().isEmpty(), "阶段 1 至少应能发现当前模块结构");
    }
}

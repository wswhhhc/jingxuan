package com.jingxuan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestContainerImagesTest {

    @Test
    void shouldKeepDefaultMysqlImageWithoutOverrides() {
        assertEquals(TestContainerImages.DEFAULT_MYSQL_IMAGE,
                TestContainerImages.resolveImageName(null, null, TestContainerImages.DEFAULT_MYSQL_IMAGE));
    }

    @Test
    void shouldKeepDefaultRedisImageWithoutOverrides() {
        assertEquals(TestContainerImages.DEFAULT_REDIS_IMAGE,
                TestContainerImages.resolveImageName(null, null, TestContainerImages.DEFAULT_REDIS_IMAGE));
    }

    @Test
    void shouldApplyRegistryPrefixToOfficialLibraryImages() {
        assertEquals("mirror.example/library/mysql:8.0.42",
                TestContainerImages.resolveImageName(null, "mirror.example", TestContainerImages.DEFAULT_MYSQL_IMAGE));
    }

    @Test
    void shouldApplyRegistryPrefixToNamespacedImages() {
        assertEquals("mirror.example/testcontainers/ryuk:0.6.0",
                TestContainerImages.resolveImageName(null, "mirror.example", "testcontainers/ryuk:0.6.0"));
    }

    @Test
    void shouldPreferExplicitImageOverRegistryPrefix() {
        assertEquals("registry.local/custom/mysql:8.0.42",
                TestContainerImages.resolveImageName("registry.local/custom/mysql:8.0.42", "mirror.example",
                        TestContainerImages.DEFAULT_MYSQL_IMAGE));
    }
}

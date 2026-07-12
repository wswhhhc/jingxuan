package com.jingxuan;

import org.testcontainers.utility.DockerImageName;

public final class TestContainerImages {

    static final String DEFAULT_MYSQL_IMAGE = "mysql:8.0.42";
    static final String DEFAULT_REDIS_IMAGE = "redis:7.4.5-alpine";

    private static final String IMAGE_REGISTRY_PREFIX_ENV = "JINGXUAN_TEST_IMAGE_REGISTRY";
    private static final String MYSQL_IMAGE_ENV = "JINGXUAN_TEST_MYSQL_IMAGE";
    private static final String REDIS_IMAGE_ENV = "JINGXUAN_TEST_REDIS_IMAGE";

    private TestContainerImages() {
    }

    public static DockerImageName mysql() {
        return parseConfiguredImage(MYSQL_IMAGE_ENV, DEFAULT_MYSQL_IMAGE);
    }

    public static DockerImageName redis() {
        return parseConfiguredImage(REDIS_IMAGE_ENV, DEFAULT_REDIS_IMAGE);
    }

    static String resolveImageName(String imageEnvName, String defaultImage) {
        return resolveImageName(
                System.getenv(imageEnvName),
                System.getenv(IMAGE_REGISTRY_PREFIX_ENV),
                defaultImage
        );
    }

    static String resolveImageName(String explicitImage, String registryPrefix, String defaultImage) {
        String sanitizedExplicitImage = trimToNull(explicitImage);
        if (sanitizedExplicitImage != null) {
            return sanitizedExplicitImage;
        }

        String sanitizedRegistryPrefix = trimToNull(registryPrefix);
        if (sanitizedRegistryPrefix == null) {
            return defaultImage;
        }
        return joinRegistryPrefix(sanitizedRegistryPrefix, defaultImage);
    }

    private static DockerImageName parseConfiguredImage(String imageEnvName, String defaultImage) {
        String resolvedImage = resolveImageName(imageEnvName, defaultImage);
        DockerImageName imageName = DockerImageName.parse(resolvedImage);
        return imageName.asCompatibleSubstituteFor(defaultImage);
    }

    private static String joinRegistryPrefix(String registryPrefix, String defaultImage) {
        String normalizedPrefix = registryPrefix.endsWith("/") ? registryPrefix.substring(0, registryPrefix.length() - 1) : registryPrefix;
        if (defaultImage.contains("/")) {
            return normalizedPrefix + "/" + defaultImage;
        }
        return normalizedPrefix + "/library/" + defaultImage;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

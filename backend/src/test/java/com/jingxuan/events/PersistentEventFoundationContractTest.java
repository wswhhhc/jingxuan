package com.jingxuan.events;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentEventFoundationContractTest {

    private static final String JDBC_STARTER = "spring-modulith-starter-jdbc";
    private static final String MIGRATION = "db/migration/V2__spring_modulith_event_publication.sql";

    @Test
    void usesTheOfficialJdbcEventPublicationStarter() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);

        Document pom = factory.newDocumentBuilder().parse(Path.of("pom.xml").toFile());
        NodeList dependencies = pom.getElementsByTagName("dependency");

        boolean hasJdbcStarter = false;
        for (int index = 0; index < dependencies.getLength(); index++) {
            Element dependency = (Element) dependencies.item(index);
            String groupId = childText(dependency, "groupId");
            String artifactId = childText(dependency, "artifactId");
            if (isDirectProjectDependency(dependency)
                    && "org.springframework.modulith".equals(groupId)
                    && JDBC_STARTER.equals(artifactId)) {
                hasJdbcStarter = true;
                break;
            }
        }

        assertTrue(hasJdbcStarter, "持久化事务事件必须使用 Spring Modulith JDBC starter");
    }

    @Test
    void delegatesSchemaOwnershipToFlywayAndKeepsFailedPublicationsRetryable() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));
        PropertySource<?> application = sources.get(0);

        assertEquals(Boolean.FALSE,
                application.getProperty("spring.modulith.events.jdbc.schema-initialization.enabled"));
        assertEquals("delete", application.getProperty("spring.modulith.events.completion-mode"));
        assertEquals("${EVENT_RECOVERY_ENABLED:true}",
                application.getProperty("spring.modulith.events.republish-outstanding-events-on-restart"));
    }

    @Test
    void shipsTheModulithV2MysqlSchemaAsAFlywayMigration() throws Exception {
        ClassPathResource migration = new ClassPathResource(MIGRATION);
        assertTrue(migration.exists(), "Spring Modulith 事件发布表必须由 Flyway V2 创建");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8).toUpperCase();
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS EVENT_PUBLICATION"));
        assertTrue(sql.contains("LISTENER_ID"));
        assertTrue(sql.contains("EVENT_TYPE"));
        assertTrue(sql.contains("SERIALIZED_EVENT"));
        assertTrue(sql.contains("STATUS"));
        assertTrue(sql.contains("COMPLETION_ATTEMPTS"));
        assertTrue(sql.contains("LAST_RESUBMISSION_DATE"));
        assertTrue(sql.contains("EVENT_PUBLICATION_BY_COMPLETION_DATE_IDX"));
        assertFalse(sql.contains("EVENT_PUBLICATION_ARCHIVE"),
                "DELETE 完成策略不应要求未使用的归档表");
    }

    @Test
    void integrationApiContextUsesFlywayBeforeLoadingSqlFixtures() throws Exception {
        String source = Files.readString(
                Path.of("src/test/java/com/jingxuan/BaseApiTest.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("spring.flyway.enabled"),
                "完整 API 测试上下文必须在刷新期间由 Flyway 创建事件发布表");
        assertFalse(source.contains("sql/test-schema.sql"),
                "集成测试不得继续把重复 test-schema.sql 当作 Schema 来源");
    }

    @Test
    void openApiExportExcludesJdbcPublicationRepositoryWhenFlywayIsDisabled() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);

        assertTrue(pom.contains(
                        "-Dspring.autoconfigure.exclude="
                                + "org.springframework.modulith.events.jdbc.JdbcEventPublicationAutoConfiguration"),
                "H2 OpenAPI 导出关闭 Flyway 时必须排除需要 EVENT_PUBLICATION 表的 JDBC 自动配置");
        assertTrue(pom.contains(
                        "org.springframework.modulith.events.config.EventPublicationAutoConfiguration"),
                "排除 JDBC 仓储时也必须排除依赖 EventPublicationRegistry 的核心事件自动配置");
    }

    @Test
    void reliesOnTheStartersConditionalAsyncAutoConfiguration() {
        assertFalse(Files.exists(Path.of(
                        "src/main/java/com/jingxuan/config/PersistentEventConfiguration.java")),
                "Spring Modulith 2.1 starter 已按需启用异步处理，不应重复声明全局 @EnableAsync");
    }

    private static boolean isDirectProjectDependency(Element dependency) {
        if (!(dependency.getParentNode() instanceof Element dependencies)
                || !"dependencies".equals(dependencies.getTagName())) {
            return false;
        }
        return dependencies.getParentNode() instanceof Element project
                && "project".equals(project.getTagName());
    }

    private static String childText(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        return children.getLength() == 0 ? "" : children.item(0).getTextContent().trim();
    }
}

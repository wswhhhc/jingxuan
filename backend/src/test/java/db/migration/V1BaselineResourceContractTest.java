package db.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V1BaselineResourceContractTest {

    @Test
    void packagesEveryReferencedLegacyScriptWithoutProductionTestFixtures() throws Exception {
        Field scriptsField = V1__Baseline.class.getDeclaredField("BASELINE_SCRIPTS");
        scriptsField.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> scripts = (List<String>) scriptsField.get(null);

        assertEquals(14, scripts.size(), "V1 基线脚本清单发生变化时必须显式审查");
        for (String script : scripts) {
            assertTrue(script.startsWith("legacy-sql/"), () -> "基线脚本必须位于 legacy-sql: " + script);
            assertFalse(script.matches(".*test[-_]data.*"), () -> "测试 fixture 不得进入主资源: " + script);

            ClassPathResource resource = new ClassPathResource(script);
            assertTrue(resource.exists(), () -> "V1 引用的资源未随应用打包: " + script);
            assertTrue(resource.isReadable(), () -> "V1 引用的资源不可读: " + script);
        }

        assertFalse(Files.exists(Path.of(
                "src/main/resources/legacy-sql/business/test_data.sql")));
        assertFalse(Files.exists(Path.of(
                "src/main/resources/legacy-sql/business/test-data.sql")));
        assertFalse(Files.exists(Path.of("src/test/resources/schema-test.sql")),
                "测试 Schema 必须由 Flyway 创建，不得保留重复 H2 建表脚本");
        assertFalse(Files.exists(Path.of("src/test/resources/data-test.sql")),
                "测试数据只允许保留显式使用的 sql/test-data.sql fixture");
        assertFalse(Files.exists(Path.of("src/test/resources/test-schema.sql")),
                "不得恢复已删除的重复测试 Schema");
    }
}

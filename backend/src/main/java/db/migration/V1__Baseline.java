package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 阶段 1 过渡基线：把现有已验证 SQL 收拢到 Flyway 管理下。
 *
 * <p>后续阶段会逐步把 legacy SQL 内联拆成标准迁移文件；在此之前先确保空库启动
 * 由 Flyway 驱动，而不是散落脚本与手工初始化。</p>
 */
public class V1__Baseline extends BaseJavaMigration {

    private static final Pattern DATABASE_DIRECTIVE = Pattern.compile(
            "(?im)^\\s*(CREATE\\s+DATABASE\\b.*?|USE\\s+\\w+\\s*;)\\s*$"
    );
    private static final Pattern SQL_COMMENTS = Pattern.compile(
            "(?s)/\\*.*?\\*/|(?m)--[^\\r\\n]*"
    );

    private static final List<String> BASELINE_SCRIPTS = List.of(
            "legacy-sql/base/init_schema.sql",
            "legacy-sql/business/work_schema.sql",
            "legacy-sql/business/2026-05-19-fix-work-attachment-nullable.sql",
            "legacy-sql/business/2026-05-24-like-fav-view-tag.sql",
            "legacy-sql/business/2026-06-02-runtime-support.sql",
            "legacy-sql/business/2026-06-03-server-preview-clean-runtime.sql",
            "legacy-sql/business/2026-06-04-email-verification.sql",
            "legacy-sql/business/2026-06-04-remove-sourcecode-concept.sql",
            "legacy-sql/business/2026-06-05-add-deleted-column.sql",
            "legacy-sql/business/2026-06-05-guest-comment.sql",
            "legacy-sql/business/2026-06-06-batch-notice.sql",
            "legacy-sql/business/2026-06-08-delete-request.sql",
            "legacy-sql/business/2026-06-08-student-task.sql",
            "legacy-sql/business/2026-06-10-notice-target-scope.sql"
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        for (String scriptPath : BASELINE_SCRIPTS) {
            String sql = readSanitizedSql(scriptPath);
            if (!hasExecutableStatements(sql)) {
                continue;
            }
            ScriptUtils.executeSqlScript(
                    connection,
                    new EncodedResource(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)))
            );
        }
    }

    private static boolean hasExecutableStatements(String sql) {
        return !SQL_COMMENTS.matcher(sql).replaceAll("").isBlank();
    }

    private String readSanitizedSql(String scriptPath) throws IOException {
        String sql = new ClassPathResource(scriptPath).getContentAsString(StandardCharsets.UTF_8);
        return DATABASE_DIRECTIVE.matcher(sql).replaceAll("");
    }
}

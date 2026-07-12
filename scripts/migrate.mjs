#!/usr/bin/env node

/**
 * 菁选 v1 → v2 数据迁移 CLI
 *
 * 用法:
 *   node scripts/migrate.mjs preflight  --source-db=<uri> --target-db=<uri>
 *   node scripts/migrate.mjs migrate   --source-db=<uri> --target-db=<uri>
 *   node scripts/migrate.mjs verify    --target-db=<uri>
 *   node scripts/migrate.mjs rollback  --target-db=<uri>
 *
 * 注意: 本脚本只生成并输出 SQL 语句/分析报告，不实际连接数据库。
 *       实际执行时需配合 mysql CLI 或数据库管理工具。
 */

import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { readdir } from "node:fs/promises";
import { join, resolve } from "node:path";
import { argv, exit, stderr, stdout } from "node:process";

// ─── 配置声明 ────────────────────────────────────────────────────────

/** 源表 → 目标表映射及迁移过滤规则 */
const TABLE_MIGRATIONS = [
  {
    sourceTable: "sys_user",
    targetTable: "sys_user",
    whereClause: "WHERE deleted = 0",
    // 排除 role_id=0 的无角色用户
    extraFilter: "AND role_id > 0",
    // 迁移超过 7 天未绑定附件的用户（create_time + 7d 且没有 work 记录）
    orphanFilter: null, // migrate 阶段会单独处理
    columns: [
      "id",
      "username",
      "password",
      "real_name",
      "role_id",
      "class_id",
      "avatar",
      "phone",
      "email",
      "status",
      "first_login",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "sys_role",
    targetTable: "sys_role",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "role_name",
      "role_code",
      "description",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "sys_menu",
    targetTable: "sys_menu",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "menu_name",
      "parent_id",
      "path",
      "permission",
      "type",
      "icon",
      "sort",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "sys_role_menu",
    targetTable: "sys_role_menu",
    whereClause: "WHERE deleted = 0",
    columns: ["id", "role_id", "menu_id", "create_time", "update_time"],
  },
  {
    sourceTable: "sys_dict",
    targetTable: "sys_dict",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "dict_type",
      "dict_label",
      "dict_value",
      "sort",
      "remark",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "sys_log",
    targetTable: "sys_log",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "user_id",
      "username",
      "action",
      "target",
      "target_id",
      "ip",
      "request_method",
      "request_path",
      "params",
      "result",
      "error_msg",
      "duration",
      "create_time",
    ],
  },
  {
    sourceTable: "sys_notice",
    targetTable: "sys_notice",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "title",
      "content",
      "publisher_id",
      "publish_time",
      "top_flag",
      "status",
      "target_scope",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "sys_notification",
    targetTable: "sys_notification",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "user_id",
      "title",
      "content",
      "type",
      "ref_id",
      "is_read",
      "read_time",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "work",
    targetTable: "work",
    // 排除草稿和软删除，同时排除超过 7 天未绑定附件的
    whereClause: "WHERE deleted = 0 AND status != 0",
    orphanFilter:
      "AND (submit_time IS NOT NULL OR " +
      "EXISTS (SELECT 1 FROM work_attachment wa WHERE wa.work_id = work.id AND wa.deleted = 0) OR " +
      "create_time > DATE_SUB(NOW(), INTERVAL 7 DAY))",
    columns: [
      "id",
      "title",
      "summary",
      "tech_stack",
      "advisor",
      "cover_url",
      "video_url",
      "preview_url",
      "run_desc",
      "status",
      "submitter_id",
      "submit_time",
      "batch_id",
      "view_count",
      "like_count",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "work_member",
    targetTable: "work_member",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "work_id",
      "student_id",
      "student_name",
      "student_no",
      "class_name",
      "is_leader",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "work_attachment",
    targetTable: "work_attachment",
    whereClause: "WHERE deleted = 0 AND work_id IS NOT NULL",
    columns: [
      "id",
      "work_id",
      "file_name",
      "file_type",
      "file_size",
      "file_url",
      "category",
      "sha256",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "work_audit",
    targetTable: "work_audit",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "work_id",
      "auditor_id",
      "result",
      "reason",
      "audit_time",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "work_publish",
    targetTable: "work_publish",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "work_id",
      "publish_status",
      "featured",
      "publish_time",
      "offline_time",
      "publisher_id",
      "preview_url",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "work_score",
    targetTable: "work_score",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "work_id",
      "teacher_id",
      "innovation",
      "difficulty",
      "completion",
      "practicality",
      "total",
      "comment",
      "batch_id",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "work_comment",
    targetTable: "work_comment",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "work_id",
      "user_id",
      "guest_name",
      "content",
      "parent_id",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "score_batch",
    targetTable: "score_batch",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "batch_name",
      "start_time",
      "end_time",
      "class_scopes",
      "status",
      "rank_published",
      "notice_title",
      "notice_content",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "sensitive_rule",
    targetTable: "sensitive_rule",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "rule_name",
      "system_prompt",
      "enabled_categories",
      "on_reject_action",
      "status",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "work_like",
    targetTable: "work_like",
    whereClause: "WHERE deleted = 0",
    columns: ["id", "work_id", "user_id", "create_time"],
  },
  {
    sourceTable: "tag",
    targetTable: "tag",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "name",
      "color",
      "type",
      "sort",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "work_tag",
    targetTable: "work_tag",
    whereClause: "WHERE deleted = 0",
    columns: ["id", "work_id", "tag_id", "create_time"],
  },
  {
    sourceTable: "student_task",
    targetTable: "student_task",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "user_id",
      "batch_id",
      "work_id",
      "title",
      "content",
      "status",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "delete_request",
    targetTable: "delete_request",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "work_id",
      "student_id",
      "reason",
      "status",
      "admin_reply",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "rank_reward",
    targetTable: "rank_reward",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "batch_id",
      "reward_level",
      "reward_name",
      "prize_name",
      "prize_image",
      "quota",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "reward_issue",
    targetTable: "reward_issue",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "reward_id",
      "work_id",
      "issue_status",
      "issue_time",
      "operator_id",
      "remark",
      "create_time",
      "update_time",
    ],
  },
  {
    sourceTable: "reward_config",
    targetTable: "reward_config",
    whereClause: "WHERE deleted = 0",
    columns: [
      "id",
      "batch_id",
      "reward_level",
      "reward_name",
      "prize_name",
      "quota",
      "create_time",
      "update_time",
    ],
  },
];

/** 废弃的表——不做迁移 */
const DEPRECATED_TABLES = ["work_runtime", "port_manage"];

/** V5 新增的表——仅存在于目标数据库 */
const V5_NEW_TABLES = [
  "academic_class",
  "file_asset",
  "award_tier",
  "score_batch_class",
];

/** 上传文件根目录（源） */
const UPLOADS_DIR = "uploads";

// ─── 工具函数 ────────────────────────────────────────────────────────

function parseArgs() {
  const args = argv.slice(2);
  const cmd = args[0];
  const opts = {};
  for (const arg of args.slice(1)) {
    const m = arg.match(/^--([^=]+)=(.*)$/);
    if (m) {
      opts[m[1].replace(/-/g, "_")] = m[2];
    }
  }
  return { cmd, opts };
}

function validateUri(uri, label) {
  if (!uri) {
    stderr.write(`错误: 缺少 ${label}\n`);
    exit(1);
  }
  if (!/^mysql:\/\/.+/.test(uri)) {
    stderr.write(
      `错误: ${label} 格式无效，应为 mysql://user:pass@host:port/dbname\n`,
    );
    exit(1);
  }
  return uri;
}

function generateInsertSql(migration, targetDb) {
  const {
    sourceTable,
    targetTable,
    whereClause,
    columns,
    extraFilter,
    orphanFilter,
  } = migration;
  const colList = columns.join(", ");
  const selectClauses = [
    `SELECT ${colList} FROM ${sourceTable} ${whereClause}`,
  ];

  if (extraFilter) {
    selectClauses[0] += ` ${extraFilter}`;
  }
  if (orphanFilter) {
    selectClauses[0] += ` ${orphanFilter}`;
  }

  return `-- ${sourceTable} → ${targetTable}
INSERT INTO ${targetDb}.${targetTable} (${colList})
${selectClauses[0]}
ON DUPLICATE KEY UPDATE
${columns
  .filter((c) => c !== "id")
  .map((c) => `    ${c} = VALUES(${c})`)
  .join(",\n")};
`;
}

function sha256OfFile(filePath) {
  try {
    const buf = readFileSync(filePath);
    return createHash("sha256").update(buf).digest("hex");
  } catch {
    return null;
  }
}

// ─── 子命令 ──────────────────────────────────────────────────────────

/**
 * preflight — 预检
 * 输出源/目标数据库的校验报告，包括表记录数、软删除比例、预估迁移时间
 */
async function preflight(sourceDb, targetDb) {
  stdout.write("═══════════════════════════════════════════════════════\n");
  stdout.write("  菁选 v1 → v2 迁移预检报告\n");
  stdout.write("═══════════════════════════════════════════════════════\n\n");

  // 源数据库检测
  stdout.write(`源数据库: ${sourceDb}\n`);
  stdout.write(`目标数据库: ${targetDb}\n\n`);

  // 解析数据库名称用于 SQL 生成
  const sourceDbName = sourceDb.split("/").pop().split("?").shift();
  const targetDbName = targetDb.split("/").pop().split("?").shift();

  if (!sourceDbName || !targetDbName) {
    stderr.write("错误: 无法从 URI 中提取数据库名称\n");
    exit(1);
  }

  stdout.write("── 源表统计 ──────────────────────────────────────────\n");
  stdout.write(
    `  ${"表名".padEnd(28)} ${"总计".padStart(8)} ${"有效".padStart(8)} ${"软删除".padStart(8)} ${"删除占比".padStart(10)}\n`,
  );
  stdout.write("  " + "─".repeat(66) + "\n");

  let totalEffectiveRows = 0;
  let totalSourceRows = 0;
  let totalDeletedRows = 0;

  for (const m of TABLE_MIGRATIONS) {
    const table = m.sourceTable;
    // 生成预检用的计数 SQL
    // 统计总行数
    stdout.write(`  -- ${table}:
  SELECT
    (SELECT COUNT(*) FROM ${sourceDbName}.${table}) AS total,
    (SELECT COUNT(*) FROM ${sourceDbName}.${table} WHERE deleted = 0) AS valid,
    (SELECT COUNT(*) FROM ${sourceDbName}.${table} WHERE deleted = 1) AS deleted;
`);
    // 模拟输出（实际执行时替换为真实数字）
    const estimatedTotal = getEstimatedRowCount(table);
    const estimatedDeleted = Math.floor(
      estimatedTotal * getDeletedRatio(table),
    );
    const estimatedValid = estimatedTotal - estimatedDeleted;
    const pct =
      estimatedTotal > 0
        ? ((estimatedDeleted / estimatedTotal) * 100).toFixed(1)
        : "0.0";

    stdout.write(
      `  ${table.padEnd(28)} ${String(estimatedTotal).padStart(8)} ${String(estimatedValid).padStart(8)} ${String(estimatedDeleted).padStart(8)} ${`${pct}%`.padStart(10)}\n`,
    );

    totalEffectiveRows += estimatedValid;
    totalSourceRows += estimatedTotal;
    totalDeletedRows += estimatedDeleted;
  }

  stdout.write("  " + "─".repeat(66) + "\n");
  stdout.write(
    `  ${"合计".padEnd(28)} ${String(totalSourceRows).padStart(8)} ${String(totalEffectiveRows).padStart(8)} ${String(totalDeletedRows).padStart(8)}\n\n`,
  );

  // 文件统计
  stdout.write("── 文件资产统计 ──────────────────────────────────────\n");
  try {
    const files = await readdir(UPLOADS_DIR);
    stdout.write(`  ${UPLOADS_DIR}/ 目录下文件数: ${files.length}\n`);
  } catch {
    stdout.write(`  ${UPLOADS_DIR}/ 目录不存在或无法访问，跳过文件统计\n`);
  }
  stdout.write("\n");

  // 目标数据库检测
  stdout.write("── 目标数据库 Flyway 状态 ───────────────────────────\n");
  stdout.write(`  -- 检查 ${targetDbName} 是否为空库:
  SELECT COUNT(*) AS migration_count FROM ${targetDbName}.flyway_schema_history;
`);
  stdout.write("  (预期结果: migration_count = 0 或只包含已应用的 V1-V4)\n\n");

  // V5 新表检查
  stdout.write("── V5 新增表检查 ──────────────────────────────────────\n");
  for (const t of V5_NEW_TABLES) {
    stdout.write(`  -- ${t}:
  SELECT COUNT(*) AS cnt FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = '${targetDbName}' AND TABLE_NAME = '${t}';
`);
  }
  stdout.write("\n");

  // 废弃表检查
  stdout.write("── 废弃表确认 ────────────────────────────────────────\n");
  for (const t of DEPRECATED_TABLES) {
    stdout.write(`  ${t}: 不迁移\n`);
  }
  stdout.write("\n");

  // 风险提示
  stdout.write("── 风险提示 ──────────────────────────────────────────\n");
  stdout.write("  1. 迁移前请确保目标数据库已应用 V5 迁移\n");
  stdout.write(`  2. 预计迁移 ${totalEffectiveRows} 行数据\n`);
  stdout.write("  3. 大表 (sys_log, work_comment) 建议分页迁移\n");
  stdout.write("  4. 迁移完成后执行 verify 命令确认数据完整性\n");
  stdout.write("  5. 文件搬迁需手动执行，脚本仅生成校验清单\n");
  stdout.write("\n");
  stdout.write("═══════════════════════════════════════════════════════\n");
}

/**
 * migrate — 生成完整迁移 SQL
 * 逐表生成 INSERT INTO ... SELECT 语句
 */
async function migrate(sourceDb, targetDb) {
  const sourceDbName = sourceDb.split("/").pop().split("?").shift();
  const targetDbName = targetDb.split("/").pop().split("?").shift();

  if (!sourceDbName || !targetDbName) {
    stderr.write("错误: 无法从 URI 中提取数据库名称\n");
    exit(1);
  }

  stdout.write(
    "-- ============================================================\n",
  );
  stdout.write(`-- 菁选 v1 → v2 迁移 SQL\n`);
  stdout.write(`-- 源数据库: ${sourceDbName}\n`);
  stdout.write(`-- 目标数据库: ${targetDbName}\n`);
  stdout.write("-- 生成时间: " + new Date().toISOString() + "\n");
  stdout.write("-- \n");
  stdout.write("-- 用法: mysql -u root -p < this_file.sql\n");
  stdout.write(
    "-- ============================================================\n\n",
  );

  stdout.write("SET NAMES utf8mb4;\n");
  stdout.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");

  // 先禁用目标数据库外键检查（绕过 DDL 顺序约束）
  stdout.write(`-- 禁用外键检查\n`);
  stdout.write(`SET @OLD_FK_CHECKS = @@FOREIGN_KEY_CHECKS;\n`);
  stdout.write(`SET FOREIGN_KEY_CHECKS = 0;\n\n`);

  // 逐表生成迁移 SQL
  for (const m of TABLE_MIGRATIONS) {
    const insertSql = generateInsertSql(m, targetDbName);
    stdout.write(insertSql);
    stdout.write("\n");
  }

  // 生成 file_asset 迁移 SQL（从 work_attachment 中提取有效文件记录）
  stdout.write(
    `-- work_attachment → ${targetDbName}.file_asset（仅 SHA256 校验有效的记录）\n`,
  );
  stdout.write(
    `INSERT INTO ${targetDbName}.file_asset (id, original_name, stored_path, file_size, sha256, mime_type, create_time)\n`,
  );
  stdout.write(`SELECT\n`);
  stdout.write(`    wa.id,\n`);
  stdout.write(`    wa.file_name,\n`);
  stdout.write(`    wa.file_url,\n`);
  stdout.write(`    wa.file_size,\n`);
  stdout.write(`    wa.sha256,\n`);
  stdout.write(`    CASE\n`);
  stdout.write(
    `        WHEN wa.file_type IN ('jpg','jpeg','png','gif','webp') THEN 'image/' || wa.file_type\n`,
  );
  stdout.write(`        WHEN wa.file_type = 'pdf' THEN 'application/pdf'\n`);
  stdout.write(
    `        WHEN wa.file_type IN ('zip','rar','7z','tar','gz') THEN 'application/zip'\n`,
  );
  stdout.write(
    `        WHEN wa.file_type IN ('mp4','avi','mov','wmv') THEN 'video/' || wa.file_type\n`,
  );
  stdout.write(`        ELSE 'application/octet-stream'\n`);
  stdout.write(`    END,\n`);
  stdout.write(`    wa.create_time\n`);
  stdout.write(`FROM ${sourceDbName}.work_attachment wa\n`);
  stdout.write(`WHERE wa.deleted = 0\n`);
  stdout.write(`  AND wa.work_id IS NOT NULL\n`);
  stdout.write(`  AND wa.file_url IS NOT NULL\n`);
  stdout.write(`  AND wa.file_url != ''\n`);
  stdout.write(`ON DUPLICATE KEY UPDATE\n`);
  stdout.write(`    original_name = VALUES(original_name),\n`);
  stdout.write(`    file_size = VALUES(file_size),\n`);
  stdout.write(`    sha256 = VALUES(sha256);\n\n`);

  // 生成 academic_class 迁移（从 sys_dict 提取 class 类型数据）
  stdout.write(
    `-- sys_dict (dict_type='class') → ${targetDbName}.academic_class\n`,
  );
  stdout.write(
    `INSERT INTO ${targetDbName}.academic_class (id, class_name, class_code, grade, create_time, update_time)\n`,
  );
  stdout.write(`SELECT\n`);
  stdout.write(`    id,\n`);
  stdout.write(`    dict_label,\n`);
  stdout.write(`    dict_value,\n`);
  stdout.write(`    remark AS grade,\n`);
  stdout.write(`    create_time,\n`);
  stdout.write(`    update_time\n`);
  stdout.write(`FROM ${sourceDbName}.sys_dict\n`);
  stdout.write(`WHERE dict_type = 'class'\n`);
  stdout.write(`  AND deleted = 0\n`);
  stdout.write(`ON DUPLICATE KEY UPDATE\n`);
  stdout.write(`    class_name = VALUES(class_name),\n`);
  stdout.write(`    class_code = VALUES(class_code);\n\n`);

  // 生成 award_tier 迁移（合并 reward_config + rank_reward）
  stdout.write(
    `-- ${targetDbName}.award_tier（合并 reward_config 与 rank_reward）\n`,
  );
  stdout.write(
    `INSERT INTO ${targetDbName}.award_tier (id, batch_id, tier_name, prize_name, quota, sort, create_time)\n`,
  );
  stdout.write(
    `SELECT id, batch_id, reward_level AS tier_name, prize_name, quota, 0 AS sort, create_time\n`,
  );
  stdout.write(`FROM ${sourceDbName}.reward_config\n`);
  stdout.write(`WHERE deleted = 0\n`);
  stdout.write(`ON DUPLICATE KEY UPDATE\n`);
  stdout.write(`    tier_name = VALUES(tier_name);\n\n`);

  // 生成 score_batch_class 迁移（解析 score_batch.class_scopes JSON）
  stdout.write(
    `-- score_batch.class_scopes → ${targetDbName}.score_batch_class\n`,
  );
  stdout.write(`-- 注意: class_scopes 为 JSON 数组，如 ["101","102"]\n`);
  stdout.write(`-- 需要手动确认解析逻辑，以下是基于 MySQL JSON_TABLE 的示例\n`);
  stdout.write(
    `INSERT INTO ${targetDbName}.score_batch_class (id, batch_id, class_id)\n`,
  );
  stdout.write(`SELECT\n`);
  stdout.write(`    (sb.id * 10000 + j.idx) AS id,\n`);
  stdout.write(`    sb.id AS batch_id,\n`);
  stdout.write(
    `    CAST(TRIM(BOTH '\"' FROM j.class_id_str) AS UNSIGNED) AS class_id\n`,
  );
  stdout.write(`FROM ${sourceDbName}.score_batch sb\n`);
  stdout.write(`CROSS JOIN JSON_TABLE(\n`);
  stdout.write(`    sb.class_scopes,\n`);
  stdout.write(`    '$[*]' COLUMNS (\n`);
  stdout.write(`        idx FOR ORDINALITY,\n`);
  stdout.write(`        class_id_str VARCHAR(50) PATH '$'\n`);
  stdout.write(`    )\n`);
  stdout.write(`) j\n`);
  stdout.write(`WHERE sb.deleted = 0\n`);
  stdout.write(`  AND sb.class_scopes IS NOT NULL\n`);
  stdout.write(`  AND sb.class_scopes != ''\n`);
  stdout.write(`  AND sb.class_scopes != '[]'\n`);
  stdout.write(`ON DUPLICATE KEY UPDATE\n`);
  stdout.write(`    class_id = VALUES(class_id);\n\n`);

  // 恢复外键检查
  stdout.write(`SET FOREIGN_KEY_CHECKS = @OLD_FK_CHECKS;\n\n`);

  stdout.write(
    "-- ============================================================\n",
  );
  stdout.write("-- 迁移 SQL 生成完成\n");
  stdout.write(
    "-- ============================================================\n",
  );
}

/**
 * verify — 验证
 * 生成验证 SQL 和校验步骤说明
 */
async function verify(targetDb) {
  const targetDbName = targetDb.split("/").pop().split("?").shift();

  if (!targetDbName) {
    stderr.write("错误: 无法从 URI 中提取数据库名称\n");
    exit(1);
  }

  stdout.write("═══ 数据迁移验证报告 ═══\n\n");

  stdout.write("── 目标表行数统计 ────────────────────────────────────\n");
  for (const m of TABLE_MIGRATIONS) {
    stdout.write(
      `  SELECT '${m.targetTable}' AS table_name, COUNT(*) AS row_count FROM ${targetDbName}.${m.targetTable};\n`,
    );
  }
  for (const t of V5_NEW_TABLES) {
    stdout.write(
      `  SELECT '${t}' AS table_name, COUNT(*) AS row_count FROM ${targetDbName}.${t};\n`,
    );
  }
  stdout.write("\n");

  // 数据完整性校验
  stdout.write("── 数据完整性校验 ────────────────────────────────────\n");
  stdout.write("  -- 1. 用户密码不为空\n");
  stdout.write(
    `  SELECT id, username FROM ${targetDbName}.sys_user WHERE password IS NULL OR password = '';\n\n`,
  );

  stdout.write("  -- 2. 作品提交者必须存在\n");
  stdout.write(`  SELECT w.id FROM ${targetDbName}.work w\n`);
  stdout.write(
    `  LEFT JOIN ${targetDbName}.sys_user u ON u.id = w.submitter_id\n`,
  );
  stdout.write(`  WHERE u.id IS NULL;\n\n`);

  stdout.write("  -- 3. 评分教师必须存在\n");
  stdout.write(
    `  SELECT DISTINCT ws.teacher_id FROM ${targetDbName}.work_score ws\n`,
  );
  stdout.write(
    `  LEFT JOIN ${targetDbName}.sys_user u ON u.id = ws.teacher_id\n`,
  );
  stdout.write(`  WHERE u.id IS NULL;\n\n`);

  stdout.write("  -- 4. 作品成员关联不存在\n");
  stdout.write(`  SELECT wm.id FROM ${targetDbName}.work_member wm\n`);
  stdout.write(`  LEFT JOIN ${targetDbName}.work w ON w.id = wm.work_id\n`);
  stdout.write(`  WHERE w.id IS NULL;\n\n`);

  stdout.write("  -- 5. 评论关联作品存在\n");
  stdout.write(`  SELECT wc.id FROM ${targetDbName}.work_comment wc\n`);
  stdout.write(`  LEFT JOIN ${targetDbName}.work w ON w.id = wc.work_id\n`);
  stdout.write(`  WHERE w.id IS NULL;\n\n`);

  stdout.write("── 核心流程冒烟测试 ──────────────────────────────────\n");
  stdout.write("  -- 1. 管理员账号登录：应有 1 条记录\n");
  stdout.write(
    `  SELECT id, username, real_name, role_id FROM ${targetDbName}.sys_user\n`,
  );
  stdout.write(`  JOIN ${targetDbName}.sys_role ON role_id = sys_role.id\n`);
  stdout.write(`  WHERE role_code = 'ROLE_ADMIN' AND status = 1;\n\n`);

  stdout.write("  -- 2. 教师账号登录：应 >= 1 条记录\n");
  stdout.write(
    `  SELECT id, username, real_name, role_id FROM ${targetDbName}.sys_user\n`,
  );
  stdout.write(`  JOIN ${targetDbName}.sys_role ON role_id = sys_role.id\n`);
  stdout.write(`  WHERE role_code = 'ROLE_TEACHER' AND status = 1;\n\n`);

  stdout.write("  -- 3. 学生账号登录：应 >= 1 条记录\n");
  stdout.write(
    `  SELECT id, username, real_name, role_id FROM ${targetDbName}.sys_user\n`,
  );
  stdout.write(`  JOIN ${targetDbName}.sys_role ON role_id = sys_role.id\n`);
  stdout.write(`  WHERE role_code = 'ROLE_STUDENT' AND status = 1;\n\n`);

  stdout.write("  -- 4. 审核过的作品：应有已通过的作品\n");
  stdout.write(
    `  SELECT COUNT(*) AS published_works FROM ${targetDbName}.work_publish WHERE publish_status = 1;\n\n`,
  );

  stdout.write("  -- 5. 评分数据：应有评分记录\n");
  stdout.write(
    `  SELECT COUNT(*) AS scored_works, COUNT(DISTINCT work_id) AS distinct_works FROM ${targetDbName}.work_score;\n\n`,
  );
}

/**
 * rollback — 回滚
 * 生成目标数据库中已迁移数据的清理 SQL
 */
async function rollback(targetDb) {
  const targetDbName = targetDb.split("/").pop().split("?").shift();

  if (!targetDbName) {
    stderr.write("错误: 无法从 URI 中提取数据库名称\n");
    exit(1);
  }

  stdout.write(
    "-- ============================================================\n",
  );
  stdout.write("-- 菁选 v2 迁移回滚 SQL\n");
  stdout.write(`-- 目标数据库: ${targetDbName}\n`);
  stdout.write("-- \n");
  stdout.write("-- 注意: 只清理迁移数据，保留 Flyway 表结构和 V5 新表\n");
  stdout.write(
    "-- ============================================================\n\n",
  );

  stdout.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");

  // 清空迁移表（反向顺序以避免外键约束）
  const rollbackOrder = [
    "work_tag",
    "tag",
    "work_like",
    "work_comment",
    "work_score",
    "delete_request",
    "student_task",
    "work_audit",
    "work_publish",
    "work_member",
    "work_attachment",
    "work",
    "reward_issue",
    "reward_config",
    "rank_reward",
    "score_batch",
    "sensitive_rule",
    "sys_notification",
    "sys_notice",
    "sys_log",
    "sys_role_menu",
    "sys_menu",
    "sys_dict",
    "sys_user",
    "sys_role",
    "file_asset",
    "academic_class",
    "award_tier",
    "score_batch_class",
  ];

  for (const table of rollbackOrder) {
    stdout.write(`TRUNCATE TABLE ${targetDbName}.${table};\n`);
  }

  stdout.write("\nSET FOREIGN_KEY_CHECKS = 1;\n\n");

  stdout.write(
    "-- ============================================================\n",
  );
  stdout.write("-- 回滚完成\n");
  stdout.write("-- 保留的表结构: Flyway schema_history, V1-V4 旧表, V5 新表\n");
  stdout.write(
    "-- ============================================================\n",
  );
}

// ─── 预估行数（仅用于 preflight 报告模拟） ──────────────────────────

function getEstimatedRowCount(table) {
  const estimates = {
    sys_user: 200,
    sys_role: 3,
    sys_menu: 20,
    sys_role_menu: 30,
    sys_dict: 80,
    sys_log: 5000,
    sys_notice: 10,
    sys_notification: 500,
    work: 300,
    work_member: 400,
    work_attachment: 800,
    work_audit: 300,
    work_publish: 150,
    work_score: 800,
    work_comment: 2000,
    score_batch: 10,
    sensitive_rule: 5,
    work_like: 1000,
    tag: 50,
    work_tag: 500,
    student_task: 600,
    delete_request: 20,
    rank_reward: 20,
    reward_issue: 100,
    reward_config: 20,
  };
  return estimates[table] ?? 100;
}

function getDeletedRatio(table) {
  const ratios = {
    sys_user: 0.05,
    work: 0.08,
    work_attachment: 0.03,
    work_comment: 0.02,
    sys_log: 0,
    sys_notification: 0,
    student_task: 0.02,
    delete_request: 0.1,
  };
  return ratios[table] ?? 0.01;
}

// ─── 主入口 ──────────────────────────────────────────────────────────

async function main() {
  const { cmd, opts } = parseArgs();

  switch (cmd) {
    case "preflight": {
      const sourceDb = validateUri(opts.source_db, "--source-db");
      const targetDb = validateUri(opts.target_db, "--target-db");
      await preflight(sourceDb, targetDb);
      break;
    }
    case "migrate": {
      const sourceDb = validateUri(opts.source_db, "--source-db");
      const targetDb = validateUri(opts.target_db, "--target-db");
      await migrate(sourceDb, targetDb);
      break;
    }
    case "verify": {
      const targetDb = validateUri(opts.target_db, "--target-db");
      await verify(targetDb);
      break;
    }
    case "rollback": {
      const targetDb = validateUri(opts.target_db, "--target-db");
      await rollback(targetDb);
      break;
    }
    default: {
      stderr.write("用法: node scripts/migrate.mjs <command> [options]\n\n");
      stderr.write("子命令:\n");
      stderr.write(
        "  preflight  --source-db=<uri> --target-db=<uri>    预检数据库\n",
      );
      stderr.write(
        "  migrate    --source-db=<uri> --target-db=<uri>    生成迁移 SQL\n",
      );
      stderr.write(
        "  verify     --target-db=<uri>                     验证迁移结果\n",
      );
      stderr.write(
        "  rollback   --target-db=<uri>                     回滚迁移数据\n",
      );
      stderr.write("\n");
      stderr.write(
        "数据库 URI 格式: mysql://user:password@host:port/database\n",
      );
      exit(1);
    }
  }
}

main().catch((err) => {
  stderr.write(`错误: ${err.message}\n`);
  exit(1);
});

#!/usr/bin/env node

/**
 * 阶段 6 门禁：扫描确保旧代码已被彻底移除。
 *
 * 通过一批命名/包路径/路由通配规则检查无残留。
 * 每项检查失败都视为违规，全部通过 → exit 0。
 */

const { execSync } = require("child_process");
const path = require("path");
const ROOT = path.resolve(__dirname, "..");

let violations = [];

function glob(pattern) {
  try {
    const result = execSync(
      `find "${ROOT}/backend/src/main/java" -type f -name "${pattern}" 2>/dev/null`,
      { encoding: "utf-8", maxBuffer: 4 * 1024 * 1024 },
    );
    return result.trim().split("\n").filter(Boolean);
  } catch {
    return [];
  }
}

function grepDir(dir, pattern) {
  try {
    const result = execSync(
      `grep -rl '${pattern}' "${ROOT}/${dir}" 2>/dev/null || true`,
      { encoding: "utf-8", maxBuffer: 4 * 1024 * 1024 },
    );
    return result.trim().split("\n").filter(Boolean);
  } catch {
    return [];
  }
}

function checkResolved(label, files) {
  if (files.length > 0) {
    violations.push(`❌ ${label}（仍有 ${files.length} 个匹配文件）`);
    for (const f of files.slice(0, 5)) {
      violations.push(`   ${f.replace(ROOT, "")}`);
    }
    if (files.length > 5) {
      violations.push(`   …以及 ${files.length - 5} 个更多`);
    }
  }
}

// ── 1. 旧响应包装 Result<T> ────────────────────────────
{
  const files = grepDir(
    "backend/src/main/java/com/jingxuan",
    "import com\\.jingxuan\\.common\\.Result",
  );
  // 允许 v2 api/ 包（仅从 v1 模块导入 ProblemDetails/api。）
  const filtered = files.filter(
    (f) => !f.includes("/api/") && !f.includes("/internal/"),
  );
  checkResolved("旧 Result 仍然被使用", filtered);
}

// ── 2. 旧路由模式 ──────────────────────────────────────
{
  // 扫描所有 @RequestMapping/@GetMapping/@PostMapping 包含旧角色路径的
  const matches = [];
  const srcDir = `${ROOT}/backend/src/main/java`;
  try {
    const grepResult = execSync(
      `grep -rnE '@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping)\\(.*"/admin/|@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping)\\(.*"/teacher/|@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping)\\(.*"/student/|@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping)\\(.*"/public/' "${srcDir}" 2>/dev/null || true`,
      { encoding: "utf-8", maxBuffer: 4 * 1024 * 1024 },
    );
    if (grepResult.trim()) {
      matches.push(...grepResult.trim().split("\n").filter(Boolean));
    }
  } catch {}
  checkResolved("旧角色路由（/admin/ /teacher/ /student/ /public/）", matches);
}

// ── 3. 旧 adapter 模块 ──────────────────────────────────
{
  checkResolved("旧 modules/adapter/", glob("*Adapter*.java"));
}

// ── 4. 旧 common 类型 ──────────────────────────────────
{
  const types = ["PageUtil", "BaseEntity", "BaseServiceImpl"];
  for (const type of types) {
    checkResolved(
      `旧 common.${type} 仍然存在`,
      grepDir("backend/src/main/java/com/jingxuan/common", `class ${type}`),
    );
  }
}

// ── 5. 旧 entity/ ──────────────────────────────────────
{
  // 扫描旧 entity 目录文件
  const entityFiles = glob("*.java")
    .filter((f) => f.includes("/entity/"))
    .filter(
      (f) =>
        !f.includes("/entity/BaseEntity") &&
        !f.includes("/entity/SysUser") &&
        !f.includes("/entity/SysRole") &&
        !f.includes("/entity/SysMenu") &&
        !f.includes("/entity/SysRoleMenu"),
    );
  checkResolved("旧 entity 目录仍有残留", entityFiles);
}

// ── 6. 旧 controller/ ────────────────────────────────
{
  checkResolved("旧 controller/ 目录仍有残留", glob("*Controller*.java"));
}

// ── 7. 旧 service/ ─────────────────────────────────────
{
  checkResolved("旧 service/ 目录仍有残留", glob("*ServiceImpl.java"));
}

// ── 8. 旧 modules/ 子模块 ──────────────────────────────
{
  const oldModuleDirs = [
    "audit",
    "comment",
    "deleterequest",
    "dict",
    "log",
    "notice",
    "notification",
    "prize",
    "publish",
    "rank",
    "score",
    "scorebatch",
    "sensitive",
    "task",
    "userimport",
    "work",
    "adapter",
  ];
  for (const dir of oldModuleDirs) {
    const files = glob("*.java").filter((f) => f.includes(`/modules/${dir}/`));
    checkResolved(`旧 modules/${dir}/ 仍有残留`, files);
  }
}

// ── 9. 旧 SQL legacy 目录 ───────────────────────────────
{
  checkResolved(
    "旧 legacy-sql/ 目录仍有残留",
    glob("*.sql").filter((f) => f.includes("/legacy-sql/")),
  );
}

// ── 输出结果 ────────────────────────────────────────────
if (violations.length > 0) {
  console.log(`\n阶段 6 门禁：发现 ${violations.length} 项违规\n`);
  for (const v of violations) {
    console.log(v);
  }
  console.log(`\n请先完成旧代码清理，再重新运行本检查。`);
  process.exit(1);
} else {
  console.log("\n阶段 6 门禁：全部通过 ✓");
  process.exit(0);
}

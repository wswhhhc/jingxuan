import { spawnSync } from "node:child_process";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const workspaceRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);

const requiredTemplateKeys = new Set([
  "SPRING_PROFILES_ACTIVE",
  "DB_HOST",
  "DB_PORT",
  "DB_NAME",
  "DB_USER",
  "DB_PASSWORD",
  "REDIS_HOST",
  "REDIS_PORT",
  "JINGXUAN_UPLOAD_PATH",
  "JWT_SECRET",
  "JWT_EXPIRATION_MS",
  "DEEPSEEK_API_KEY",
  "MAIL_HOST",
  "MAIL_PORT",
  "MAIL_USERNAME",
  "MAIL_PASSWORD",
  "MAIL_FROM",
]);

const keysThatMustStayEmpty = new Set([
  "DB_PASSWORD",
  "JWT_SECRET",
  "DEEPSEEK_API_KEY",
  "MAIL_USERNAME",
  "MAIL_PASSWORD",
  "MAIL_FROM",
]);

const requiredTemplateDefaults = new Map([
  ["DB_USER", "root"],
  ["REDIS_HOST", "localhost"],
  ["REDIS_PORT", "6379"],
  ["JINGXUAN_UPLOAD_PATH", "./uploads"],
]);

function gitCheckIgnore(relativePath) {
  const result = spawnSync(
    "git",
    ["check-ignore", "--no-index", "--quiet", "--", relativePath],
    { cwd: workspaceRoot },
  );

  if (result.error) {
    throw result.error;
  }

  return result.status === 0;
}

export function validateEnvExample(envExample) {
  const failures = [];
  const envValues = new Map();

  for (const rawLine of envExample.split(/\r?\n/u)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) continue;

    const separatorIndex = line.indexOf("=");
    if (separatorIndex < 0) continue;

    const key = line.slice(0, separatorIndex).trim();
    const value = line.slice(separatorIndex + 1).trim();
    envValues.set(key, value);
  }

  for (const key of requiredTemplateKeys) {
    if (!envValues.has(key)) {
      failures.push(`.env.example 缺少必填键 ${key}`);
    }
  }

  for (const key of keysThatMustStayEmpty) {
    if (envValues.has(key) && envValues.get(key) !== "") {
      failures.push(`.env.example 中的 ${key} 必须保持为空`);
    }
  }

  for (const [key, expectedValue] of requiredTemplateDefaults) {
    if (envValues.has(key) && envValues.get(key) !== expectedValue) {
      failures.push(`.env.example 中的 ${key} 必须为 ${expectedValue}`);
    }
  }

  if (envValues.has("DB_USERNAME")) {
    failures.push(".env.example 必须使用 DB_USER，不得使用 DB_USERNAME");
  }

  return failures;
}

async function main() {
  const failures = [];
  const fail = (message) => failures.push(message);
  const pathsThatMustBeIgnored = [
    ".env",
    ".env.test",
    "backend/.env.test",
    "secrets/server.key",
    "secrets/server.pem",
    "frontend/dist/index.html",
    "frontend/auto-imports.d.ts",
    "frontend/components.d.ts",
    "backend/target/application.jar",
    ".playwright-cli/session.json",
    ".codegraph/daemon.pid",
    "backend/META-INF/maven/pom.xml",
  ];

  for (const relativePath of pathsThatMustBeIgnored) {
    if (!gitCheckIgnore(relativePath)) {
      fail(`${relativePath} 未被 .gitignore 覆盖`);
    }
  }

  const pathsThatMustBeCommitted = [
    ".env.example",
    ".gitleaks.toml",
    ".codegraph/.gitignore",
    "frontend/src/env.d.ts",
    "output/playwright/baseline.png",
  ];
  for (const relativePath of pathsThatMustBeCommitted) {
    if (gitCheckIgnore(relativePath)) {
      fail(`${relativePath} 应当允许提交`);
    }
  }

  const envExample = await readFile(
    path.join(workspaceRoot, ".env.example"),
    "utf8",
  );
  failures.push(...validateEnvExample(envExample));

  const applicationYamlPath = path.join(
    workspaceRoot,
    "backend",
    "src",
    "main",
    "resources",
    "application.yml",
  );
  const applicationYaml = await readFile(applicationYamlPath, "utf8");
  const lines = applicationYaml.split(/\r?\n/u);
  let inJwtBlock = false;
  let jwtSecretValue = null;

  for (const line of lines) {
    if (/^jwt:\s*(?:#.*)?$/u.test(line)) {
      inJwtBlock = true;
      continue;
    }

    if (inJwtBlock && /^\S/u.test(line) && line.trim() !== "") {
      inJwtBlock = false;
    }

    if (inJwtBlock) {
      const match = line.match(/^\s+secret:\s*([^#]*?)(?:\s+#.*)?$/u);
      if (match) {
        jwtSecretValue = match[1].trim();
        break;
      }
    }
  }

  if (jwtSecretValue !== "${JWT_SECRET}") {
    fail(
      "application.yml 的 jwt.secret 必须严格使用 ${JWT_SECRET}，且不得提供默认值",
    );
  }

  if (failures.length > 0) {
    console.error("安全配置基线检查失败：");
    for (const failure of failures) {
      console.error(`- ${failure}`);
    }
    process.exitCode = 1;
    return;
  }

  console.log(
    "安全配置基线检查通过：环境文件、证书密钥与 JWT 配置均符合要求。",
  );
}

if (
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  await main();
}

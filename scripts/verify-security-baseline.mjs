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
  "DB_ROOT_PASSWORD",
  "DB_LEGACY_ROOT_PASSWORD",
  "DB_PASSWORD",
  "REDIS_HOST",
  "REDIS_PORT",
  "JINGXUAN_UPLOAD_PATH",
  "JINGXUAN_SECURITY_TRUSTED_PROXY_CIDRS",
  "JINGXUAN_DOCKER_TRUSTED_PROXY_CIDRS",
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
  "DB_ROOT_PASSWORD",
  "DB_LEGACY_ROOT_PASSWORD",
  "DB_PASSWORD",
  "JWT_SECRET",
  "DEEPSEEK_API_KEY",
  "MAIL_USERNAME",
  "MAIL_PASSWORD",
  "MAIL_FROM",
]);

const requiredTemplateDefaults = new Map([
  ["DB_USER", "jingxuan"],
  ["REDIS_HOST", "localhost"],
  ["REDIS_PORT", "6379"],
  ["JINGXUAN_UPLOAD_PATH", "./uploads"],
  ["JINGXUAN_SECURITY_TRUSTED_PROXY_CIDRS", "127.0.0.1/32,::1/128"],
  [
    "JINGXUAN_DOCKER_TRUSTED_PROXY_CIDRS",
    "127.0.0.1/32,::1/128,172.31.250.2/32",
  ],
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

export function validateComposeProxyBoundary(compose) {
  const failures = [];

  if (!compose.includes("JINGXUAN_DOCKER_TRUSTED_PROXY_CIDRS")) {
    failures.push(
      "docker-compose.yml 必须使用独立的 Docker 可信代理变量，避免被裸机配置覆盖",
    );
  }

  if (/172\.16\.0\.0\/12/u.test(compose)) {
    failures.push("docker-compose.yml 不得信任整个 Docker 私网网段");
  }

  if (!compose.includes("172.31.250.2/32")) {
    failures.push("docker-compose.yml 必须仅信任固定的 Nginx 代理地址");
  }

  if (!compose.includes("ipv4_address: 172.31.250.2")) {
    failures.push("docker-compose.yml 必须为 Nginx 配置固定代理地址");
  }

  if (!compose.includes("subnet: 172.31.250.0/29")) {
    failures.push("docker-compose.yml 必须为代理网络配置固定小网段");
  }

  if (!compose.includes('"127.0.0.1:8080:8080"')) {
    failures.push("docker-compose.yml 的后端 8080 端口必须仅绑定 127.0.0.1");
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
    "docs/refactor/assets/v1-public-works.png",
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

  const compose = await readFile(
    path.join(workspaceRoot, "docker-compose.yml"),
    "utf8",
  );
  failures.push(...validateComposeProxyBoundary(compose));

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

import { createHash } from "node:crypto";
import { existsSync } from "node:fs";
import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath, pathToFileURL } from "node:url";

const workspaceRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
const generatedRoot = path.join(
  workspaceRoot,
  "frontend",
  "src",
  "shared",
  "api",
  "generated",
);

export async function snapshotDirectory(directory = generatedRoot) {
  const snapshot = new Map();

  async function visit(currentDirectory) {
    const entries = await readdir(currentDirectory, { withFileTypes: true });
    for (const entry of entries) {
      const absolutePath = path.join(currentDirectory, entry.name);
      if (entry.isDirectory()) {
        await visit(absolutePath);
        continue;
      }

      const relativePath = path
        .relative(directory, absolutePath)
        .split(path.sep)
        .join("/");
      const hash = createHash("sha256")
        .update(await readFile(absolutePath))
        .digest("hex");
      snapshot.set(relativePath, hash);
    }
  }

  await visit(directory);
  return snapshot;
}

export function diffSnapshots(before, after) {
  const added = [...after.keys()].filter((file) => !before.has(file)).sort();
  const removed = [...before.keys()].filter((file) => !after.has(file)).sort();
  const changed = [...before.keys()]
    .filter((file) => after.has(file) && before.get(file) !== after.get(file))
    .sort();
  return { added, removed, changed };
}

export function findGeneratedUrls(source) {
  return [...source.matchAll(/\burl:\s*`(\/[^`]+)`/gu)].map(
    (match) => match[1],
  );
}

export function findGeneratedErrorTypeViolations(source) {
  const violations = [];
  if (/\bTError\s*=\s*ProblemDetails\b/u.test(source)) {
    violations.push("仍直接使用 ProblemDetails 作为 TError");
  }
  return violations;
}

async function assertGeneratedUrlsAreV1(directory = generatedRoot) {
  const urls = [];

  async function visit(currentDirectory) {
    const entries = await readdir(currentDirectory, { withFileTypes: true });
    for (const entry of entries) {
      const absolutePath = path.join(currentDirectory, entry.name);
      if (entry.isDirectory()) {
        await visit(absolutePath);
      } else if (entry.name.endsWith(".ts")) {
        urls.push(...findGeneratedUrls(await readFile(absolutePath, "utf8")));
      }
    }
  }

  await visit(directory);
  if (urls.length === 0) {
    throw new Error("生成客户端中没有发现任何 API URL");
  }

  const legacyUrls = urls.filter(
    (url) => url !== "/api/v1" && !url.startsWith("/api/v1/"),
  );
  if (legacyUrls.length > 0) {
    throw new Error(
      `生成客户端包含非 /api/v1/** URL：${[...new Set(legacyUrls)].sort().join(", ")}`,
    );
  }

  return urls.length;
}

async function assertGeneratedErrorsUseMutatorType(directory = generatedRoot) {
  const violations = [];

  async function visit(currentDirectory) {
    const entries = await readdir(currentDirectory, { withFileTypes: true });
    for (const entry of entries) {
      const absolutePath = path.join(currentDirectory, entry.name);
      if (entry.isDirectory()) {
        await visit(absolutePath);
      } else if (entry.name.endsWith(".ts")) {
        const relativePath = path
          .relative(directory, absolutePath)
          .split(path.sep)
          .join("/");
        for (const violation of findGeneratedErrorTypeViolations(
          await readFile(absolutePath, "utf8"),
        )) {
          violations.push(`${relativePath}: ${violation}`);
        }
      }
    }
  }

  await visit(directory);
  if (violations.length > 0) {
    throw new Error(
      `生成客户端错误类型未使用 mutator 的 ErrorType：\n${violations.join("\n")}`,
    );
  }
}

function formatChanges(changes) {
  return [
    ...changes.added.map((file) => `新增: ${file}`),
    ...changes.removed.map((file) => `删除: ${file}`),
    ...changes.changed.map((file) => `修改: ${file}`),
  ].join("\n");
}

async function main() {
  const before = await snapshotDirectory();
  const bundledNpmCli = path.join(
    path.dirname(process.execPath),
    "node_modules",
    "npm",
    "bin",
    "npm-cli.js",
  );
  const npmCli =
    process.env.npm_execpath ??
    (existsSync(bundledNpmCli) ? bundledNpmCli : undefined);
  const generation = npmCli
    ? spawnSync(process.execPath, [npmCli, "run", "api:generate"], {
        cwd: workspaceRoot,
        stdio: "inherit",
      })
    : spawnSync("npm", ["run", "api:generate"], {
        cwd: workspaceRoot,
        shell: process.platform === "win32",
        stdio: "inherit",
      });

  if (generation.error) {
    throw generation.error;
  }
  if (generation.status !== 0) {
    throw new Error(`API 客户端生成失败，退出码 ${generation.status}`);
  }

  const after = await snapshotDirectory();
  const changes = diffSnapshots(before, after);
  if (
    changes.added.length > 0 ||
    changes.removed.length > 0 ||
    changes.changed.length > 0
  ) {
    throw new Error(
      `生成客户端与 YAML 契约不同步，请提交本次重新生成结果：\n${formatChanges(changes)}`,
    );
  }

  const urlCount = await assertGeneratedUrlsAreV1();
  await assertGeneratedErrorsUseMutatorType();
  console.log(
    `生成客户端一致性校验通过：${after.size} 个文件，${urlCount} 个 v1 URL`,
  );
}

const isMain =
  process.argv[1] &&
  pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url;

if (isMain) {
  try {
    await main();
  } catch (error) {
    console.error(error instanceof Error ? error.message : error);
    process.exitCode = 1;
  }
}

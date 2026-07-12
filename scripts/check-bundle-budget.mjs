import { existsSync, readFileSync, statSync } from "node:fs";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { gzipSync } from "node:zlib";

export const BUNDLE_BUDGETS = Object.freeze({
  publicEntryCoreJs: 180 * 1024,
  workListFirstScreenJs: 220 * 1024,
  initialCss: 50 * 1024,
});

const WORK_LIST_ROUTE_SOURCES = [
  "src/layout/PublicLayout.vue",
  "src/views/public/WorkList.vue",
];

const workspaceRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);

function normalizeManifestPath(value) {
  return value.replaceAll("\\", "/");
}

function readManifest(distDir) {
  const candidates = [
    path.join(distDir, ".vite", "manifest.json"),
    path.join(distDir, "manifest.json"),
  ];
  const manifestPath = candidates.find((candidate) => existsSync(candidate));
  if (!manifestPath) {
    throw new Error(`未找到 Vite manifest：${candidates.join(" 或 ")}`);
  }
  return JSON.parse(readFileSync(manifestPath, "utf8"));
}

function findEntryKey(manifest) {
  const entry = Object.entries(manifest).find(([, chunk]) => chunk.isEntry);
  if (!entry) {
    throw new Error("Vite manifest 中未找到入口 chunk");
  }
  return entry[0];
}

function findSourceKey(manifest, source) {
  const normalizedSource = normalizeManifestPath(source);
  const key = Object.keys(manifest).find(
    (candidate) => normalizeManifestPath(candidate) === normalizedSource,
  );
  if (key) return key;

  const entry = Object.entries(manifest).find(
    ([, chunk]) =>
      typeof chunk.src === "string" &&
      normalizeManifestPath(chunk.src) === normalizedSource,
  );
  if (!entry) {
    throw new Error(`Vite manifest 中未找到路由源码：${source}`);
  }
  return entry[0];
}

function collectStaticChunks(manifest, rootKeys) {
  const visited = new Set();

  function visit(key) {
    if (visited.has(key)) return;
    const chunk = manifest[key];
    if (!chunk) {
      throw new Error(`Vite manifest 引用了不存在的 chunk：${key}`);
    }
    visited.add(key);
    for (const importedKey of chunk.imports ?? []) {
      visit(importedKey);
    }
  }

  rootKeys.forEach(visit);
  return visited;
}

function collectJsFiles(manifest, chunkKeys) {
  return [...chunkKeys]
    .map((key) => manifest[key].file)
    .filter((file) => typeof file === "string" && file.endsWith(".js"));
}

function collectCssFiles(manifest, chunkKeys) {
  return [
    ...new Set(
      [...chunkKeys].flatMap((key) =>
        Array.isArray(manifest[key].css) ? manifest[key].css : [],
      ),
    ),
  ];
}

function compressedSize(distDir, assetFile) {
  const assetPath = path.join(distDir, assetFile);
  const compressedPath = `${assetPath}.gz`;
  if (existsSync(compressedPath)) {
    return statSync(compressedPath).size;
  }
  if (!existsSync(assetPath)) {
    throw new Error(`manifest 资产不存在：${assetFile}`);
  }
  return gzipSync(readFileSync(assetPath)).byteLength;
}

function metric(distDir, name, files, limitBytes) {
  const uniqueFiles = [...new Set(files)].sort();
  return {
    name,
    files: uniqueFiles,
    actualBytes: uniqueFiles.reduce(
      (total, file) => total + compressedSize(distDir, file),
      0,
    ),
    limitBytes,
  };
}

export function checkBundleBudget(distDir, budgets = BUNDLE_BUDGETS) {
  const resolvedDistDir = path.resolve(distDir);
  const manifest = readManifest(resolvedDistDir);
  const entryKey = findEntryKey(manifest);
  const routeKeys = WORK_LIST_ROUTE_SOURCES.map((source) =>
    findSourceKey(manifest, source),
  );
  const entryChunks = collectStaticChunks(manifest, [entryKey]);
  const workListChunks = collectStaticChunks(manifest, [
    entryKey,
    ...routeKeys,
  ]);

  const metrics = {
    publicEntryCoreJs: metric(
      resolvedDistDir,
      "公共入口核心 JS",
      collectJsFiles(manifest, entryChunks),
      budgets.publicEntryCoreJs,
    ),
    workListFirstScreenJs: metric(
      resolvedDistDir,
      "作品列表首屏总 JS",
      collectJsFiles(manifest, workListChunks),
      budgets.workListFirstScreenJs,
    ),
    initialCss: metric(
      resolvedDistDir,
      "初始 CSS",
      collectCssFiles(manifest, entryChunks),
      budgets.initialCss,
    ),
  };
  const issues = Object.values(metrics).filter(
    ({ actualBytes, limitBytes }) => actualBytes > limitBytes,
  );

  return {
    passed: issues.length === 0,
    metrics,
    issues,
  };
}

function formatKb(bytes) {
  return (bytes / 1024).toFixed(2);
}

function printReport(report) {
  for (const { name, actualBytes, limitBytes } of Object.values(
    report.metrics,
  )) {
    console.log(
      `${name}：${formatKb(actualBytes)} KB gzip / ${formatKb(limitBytes).replace(".00", "")} KB`,
    );
  }

  if (report.passed) {
    console.log("bundle budget 校验通过");
    return;
  }

  console.error(`${report.issues.length} 项 bundle budget 超限`);
  for (const { name, actualBytes, limitBytes } of report.issues) {
    console.error(
      `- ${name}：超出 ${formatKb(actualBytes - limitBytes)} KB gzip`,
    );
  }
}

const isMain =
  process.argv[1] &&
  pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url;

if (isMain) {
  try {
    const distDir = process.argv[2]
      ? path.resolve(process.argv[2])
      : path.join(workspaceRoot, "frontend", "dist");
    const report = checkBundleBudget(distDir);
    printReport(report);
    if (!report.passed) process.exitCode = 1;
  } catch (error) {
    console.error(error instanceof Error ? error.message : String(error));
    process.exitCode = 1;
  }
}

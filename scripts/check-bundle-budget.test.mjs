import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdtemp, mkdir, rm, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

import { checkBundleBudget } from "./check-bundle-budget.mjs";

const scriptPath = fileURLToPath(
  new URL("./check-bundle-budget.mjs", import.meta.url),
);

const manifest = {
  "index.html": {
    file: "assets/entry.js",
    src: "index.html",
    isEntry: true,
    imports: ["_vendor.js"],
    dynamicImports: [
      "src/layout/PublicLayout.vue",
      "src/views/public/WorkList.vue",
    ],
    css: ["assets/entry.css"],
  },
  "_vendor.js": {
    file: "assets/vendor.js",
    css: ["assets/vendor.css"],
  },
  "_shared.js": {
    file: "assets/shared.js",
  },
  "src/layout/PublicLayout.vue": {
    file: "assets/public-layout.js",
    src: "src/layout/PublicLayout.vue",
    isDynamicEntry: true,
    imports: ["_shared.js"],
    css: ["assets/public-layout.css"],
  },
  "src/views/public/WorkList.vue": {
    file: "assets/work-list.js",
    src: "src/views/public/WorkList.vue",
    isDynamicEntry: true,
    imports: ["_shared.js"],
    css: ["assets/work-list.css"],
  },
};

async function createDist(gzipSizes) {
  const distDir = await mkdtemp(
    path.join(os.tmpdir(), "jingxuan-bundle-budget-"),
  );
  await mkdir(path.join(distDir, ".vite"), { recursive: true });
  await writeFile(
    path.join(distDir, ".vite", "manifest.json"),
    JSON.stringify(manifest),
  );

  const assetFiles = new Set(
    Object.values(manifest).flatMap((chunk) => [
      chunk.file,
      ...(chunk.css ?? []),
    ]),
  );
  for (const assetFile of assetFiles) {
    const assetPath = path.join(distDir, assetFile);
    await mkdir(path.dirname(assetPath), { recursive: true });
    await writeFile(assetPath, "fixture");
    await writeFile(`${assetPath}.gz`, Buffer.alloc(gzipSizes[assetFile] ?? 1));
  }

  return distDir;
}

test("按 manifest 静态依赖计算三项 gzip 预算并对共享 chunk 去重", async (t) => {
  const distDir = await createDist({
    "assets/entry.js": 80 * 1024,
    "assets/vendor.js": 70 * 1024,
    "assets/public-layout.js": 10 * 1024,
    "assets/work-list.js": 25 * 1024,
    "assets/shared.js": 10 * 1024,
    "assets/entry.css": 20 * 1024,
    "assets/vendor.css": 15 * 1024,
  });
  t.after(() => rm(distDir, { recursive: true, force: true }));

  const report = checkBundleBudget(distDir);

  assert.equal(report.passed, true);
  assert.equal(report.metrics.publicEntryCoreJs.actualBytes, 150 * 1024);
  assert.equal(report.metrics.workListFirstScreenJs.actualBytes, 195 * 1024);
  assert.equal(report.metrics.initialCss.actualBytes, 35 * 1024);
  assert.equal(
    report.metrics.workListFirstScreenJs.files.filter(
      (file) => file === "assets/shared.js",
    ).length,
    1,
  );
});

test("三项预算超限时返回逐项问题", async (t) => {
  const distDir = await createDist({
    "assets/entry.js": 100 * 1024,
    "assets/vendor.js": 85 * 1024,
    "assets/public-layout.js": 10 * 1024,
    "assets/work-list.js": 20 * 1024,
    "assets/shared.js": 10 * 1024,
    "assets/entry.css": 30 * 1024,
    "assets/vendor.css": 25 * 1024,
  });
  t.after(() => rm(distDir, { recursive: true, force: true }));

  const report = checkBundleBudget(distDir);

  assert.equal(report.passed, false);
  assert.deepEqual(
    report.issues.map((issue) => issue.name),
    ["公共入口核心 JS", "作品列表首屏总 JS", "初始 CSS"],
  );
});

test("CLI 在预算超限时输出实测值并返回非零状态", async (t) => {
  const distDir = await createDist({
    "assets/entry.js": 100 * 1024,
    "assets/vendor.js": 85 * 1024,
    "assets/public-layout.js": 10 * 1024,
    "assets/work-list.js": 20 * 1024,
    "assets/shared.js": 10 * 1024,
    "assets/entry.css": 30 * 1024,
    "assets/vendor.css": 25 * 1024,
  });
  t.after(() => rm(distDir, { recursive: true, force: true }));

  const result = spawnSync(process.execPath, [scriptPath, distDir], {
    encoding: "utf8",
  });

  assert.equal(result.status, 1);
  assert.match(result.stdout, /公共入口核心 JS：185\.00 KB gzip \/ 180 KB/u);
  assert.match(result.stderr, /3 项 bundle budget 超限/u);
});

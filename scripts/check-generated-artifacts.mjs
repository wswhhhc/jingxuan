#!/usr/bin/env node

import { execFile } from "node:child_process";
import path from "node:path";
import { promisify } from "node:util";
import { fileURLToPath } from "node:url";

const execFileAsync = promisify(execFile);
const scriptPath = fileURLToPath(import.meta.url);
const workspaceRoot = path.resolve(path.dirname(scriptPath), "..");

export const GENERATED_ARTIFACT_PATHS = [
  "openapi/jingxuan-v1-live.json",
  "openapi/jingxuan-v1.yaml",
  "frontend/src/shared/api/generated",
];

export function assertGeneratedArtifactsClean(statusOutput) {
  if (statusOutput.trim()) {
    const changes = statusOutput.replace(/\s+$/u, "");
    throw new Error(
      `API 生成产物未提交，请运行 npm run api:generate 并提交以下变更：\n${changes}`,
    );
  }
}

export async function main() {
  const { stdout } = await execFileAsync(
    "git",
    [
      "status",
      "--porcelain",
      "--untracked-files=all",
      "--",
      ...GENERATED_ARTIFACT_PATHS,
    ],
    { cwd: workspaceRoot, windowsHide: true },
  );
  assertGeneratedArtifactsClean(stdout);
  console.log("API 生成产物洁净性校验通过");
}

if (process.argv[1] && path.resolve(process.argv[1]) === scriptPath) {
  await main();
}

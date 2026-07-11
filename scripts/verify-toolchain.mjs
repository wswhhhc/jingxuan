import { execFileSync } from "node:child_process";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const workspaceRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
const packageJson = JSON.parse(
  await readFile(path.join(workspaceRoot, "package.json"), "utf8"),
);
const expectedNpmVersion =
  packageJson.packageManager?.match(/^npm@(.+)$/u)?.[1];

if (!expectedNpmVersion) {
  console.error("package.json 必须通过 packageManager 声明 npm 版本。");
  process.exit(1);
}

const npmVersionFromUserAgent = process.env.npm_config_user_agent?.match(
  /(?:^|\s)npm\/([^\s]+)/u,
)?.[1];
const npmExecutable = process.platform === "win32" ? "npm.cmd" : "npm";
const actualNpmVersion =
  npmVersionFromUserAgent ??
  execFileSync(npmExecutable, ["--version"], {
    cwd: workspaceRoot,
    encoding: "utf8",
  }).trim();

if (actualNpmVersion !== expectedNpmVersion) {
  console.error(
    `npm 版本不符合工作区契约：期望 ${expectedNpmVersion}，实际 ${actualNpmVersion}。`,
  );
  process.exit(1);
}

console.log(`npm 版本校验通过：${actualNpmVersion}`);

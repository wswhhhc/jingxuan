#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const REQUIRED_JAVA_MAJOR = 21;
const REQUIRED_NODE_MAJOR = 24;
const REQUIRED_NPM_VERSION = "11.11.0";

const scriptPath = fileURLToPath(import.meta.url);
const workspaceRoot = path.resolve(path.dirname(scriptPath), "..");

export function parseJavaMajor(versionOutput) {
  const match = String(versionOutput).match(
    /(?:openjdk|java)\s+version\s+"(?<major>\d+)(?:[._]\d+)?/iu,
  );
  return match?.groups?.major ? Number(match.groups.major) : null;
}

export function validateDeclaredToolchain(packageJson, pomXml) {
  const issues = [];

  if (packageJson.packageManager !== `npm@${REQUIRED_NPM_VERSION}`) {
    issues.push(`packageManager 必须锁定 npm@${REQUIRED_NPM_VERSION}`);
  }
  if (packageJson.engines?.node !== ">=24 <25") {
    issues.push("engines.node 必须为 >=24 <25");
  }
  if (packageJson.engines?.npm !== ">=11") {
    issues.push("engines.npm 必须为 >=11");
  }
  if (!/<java\.version>\s*21\s*<\/java\.version>/u.test(pomXml)) {
    issues.push("backend/pom.xml 的 java.version 必须为 21");
  }

  return issues;
}

export function validateRepositoryToolchainFiles({
  ci,
  backendDocker,
  frontendDocker,
  rootDocker,
  ecosystem,
}) {
  const issues = [];
  const ciJavaVersions = [...ci.matchAll(/java-version:\s*["']?(\d+)/gu)].map(
    ([, version]) => Number(version),
  );

  if (
    ciJavaVersions.length === 0 ||
    ciJavaVersions.some((version) => version !== REQUIRED_JAVA_MAJOR)
  ) {
    issues.push(".github/workflows/ci.yml 必须统一使用 JDK 21");
  }

  const backendUsesJava25 =
    /^FROM\s+maven:[^\s]*temurin-21(?:\s|$)/imu.test(backendDocker) &&
    /^FROM\s+eclipse-temurin:21-jre(?:-[^\s]+)?(?:\s|$)/imu.test(backendDocker);
  if (!backendUsesJava25) {
    issues.push("backend/Dockerfile 必须使用 JDK 21 构建与运行镜像");
  }

  if (!/^FROM\s+node:24(?:-[^\s]+)?(?:\s|$)/imu.test(frontendDocker)) {
    issues.push("frontend/Dockerfile 必须使用 Node 24 构建镜像");
  }

  const rootUsesJava25 =
    /^FROM\s+maven:[^\s]*temurin-21(?:\s|$)/imu.test(rootDocker) &&
    /^FROM\s+eclipse-temurin:21-jre(?:-[^\s]+)?(?:\s|$)/imu.test(rootDocker);
  const rootUsesNode24 = /^FROM\s+node:24(?:-[^\s]+)?(?:\s|$)/imu.test(
    rootDocker,
  );
  if (!rootUsesJava25 || !rootUsesNode24) {
    issues.push("根 Dockerfile 必须使用 JDK 21 与 Node 24");
  }

  if (
    ecosystem !== undefined &&
    (!/\bREQUIRED_JAVA_MAJOR\s*=\s*21\b/u.test(ecosystem) ||
      !/\bREQUIRED_NODE_MAJOR\s*=\s*24\b/u.test(ecosystem))
  ) {
    issues.push("ecosystem.config.cjs 必须声明 JDK 21 与 Node 24 LTS");
  }

  return issues;
}

function splitCiJobs(ci) {
  const lines = ci.replaceAll("\r\n", "\n").split("\n");
  const jobsLineIndex = lines.findIndex((line) => /^\s*jobs:\s*$/u.test(line));
  if (jobsLineIndex === -1) {
    return [];
  }

  const jobsIndent = lines[jobsLineIndex].match(/^\s*/u)?.[0].length ?? 0;
  const jobIndent = jobsIndent + 2;
  const jobs = [];
  let currentJob;

  for (const line of lines.slice(jobsLineIndex + 1)) {
    const trimmed = line.trim();
    const indent = line.match(/^\s*/u)?.[0].length ?? 0;

    if (trimmed && indent <= jobsIndent) {
      break;
    }

    const header = line.match(
      new RegExp(`^\\s{${jobIndent}}(?<name>[A-Za-z0-9_-]+):\\s*$`, "u"),
    );
    if (header?.groups?.name) {
      currentJob = { name: header.groups.name, lines: [line] };
      jobs.push(currentJob);
    } else if (currentJob) {
      currentJob.lines.push(line);
    }
  }

  return jobs.map(({ name, lines: jobLines }) => ({
    name,
    body: jobLines.join("\n"),
  }));
}

export function validateCiJobsRunningToolchain(ci) {
  const issues = [];

  for (const { name, body } of splitCiJobs(ci)) {
    if (!/\bverify:toolchain\b/u.test(body)) {
      continue;
    }

    const configuresJava25 =
      /uses:\s*actions\/setup-java@[^\s]+[\s\S]*?java-version:\s*["']?21["']?(?:\s|$)/iu.test(
        body,
      );
    if (!configuresJava25) {
      issues.push(`CI job ${name} 在运行 verify:toolchain 时必须配置 JDK 21`);
    }
  }

  return issues;
}

export function validateOpenApiExportJvmMemory(pomXml) {
  const profileId = "<id>openapi-export</id>";
  const idIndex = pomXml.indexOf(profileId);
  if (idIndex === -1) {
    return ["backend/pom.xml 缺少 openapi-export profile"];
  }

  const profileStart = pomXml.lastIndexOf("<profile>", idIndex);
  const profileEnd = pomXml.indexOf("</profile>", idIndex);
  const profile =
    profileStart === -1 || profileEnd === -1
      ? ""
      : pomXml.slice(profileStart, profileEnd + "</profile>".length);
  const normalizedProfile = profile.replaceAll(/\s+/gu, " ");
  const requiredArguments = [
    "-Xms128m",
    "-Xmx512m",
    "-XX:MaxMetaspaceSize=256m",
    "-XX:CompressedClassSpaceSize=128m",
  ];

  if (
    !requiredArguments.every((argument) => normalizedProfile.includes(argument))
  ) {
    return [
      "backend/pom.xml 的 openapi-export JVM 必须限制为 Xms128m/Xmx512m、MaxMetaspaceSize=256m、CompressedClassSpaceSize=128m",
    ];
  }

  return [];
}

export function validateRuntimeToolchain({
  nodeVersion,
  npmVersion,
  javaVersionOutput,
}) {
  const issues = [];
  const nodeMajor = Number.parseInt(String(nodeVersion).split(".")[0], 10);
  const javaMajor = parseJavaMajor(javaVersionOutput);

  if (nodeMajor !== REQUIRED_NODE_MAJOR) {
    issues.push(`当前 Node.js 必须为 24，实际为 ${nodeVersion || "未知"}`);
  }
  if (npmVersion !== REQUIRED_NPM_VERSION) {
    issues.push(
      `当前 npm 必须为 ${REQUIRED_NPM_VERSION}，实际为 ${npmVersion || "未知"}`,
    );
  }
  if (javaMajor !== REQUIRED_JAVA_MAJOR) {
    issues.push(`当前 Java 必须为 21，实际为 ${javaMajor ?? "无法识别"}`);
  }

  return issues;
}

function runVersionCommand(command, args, cwd) {
  const result = spawnSync(command, args, {
    cwd,
    encoding: "utf8",
    shell: process.platform === "win32" && /\.(?:cmd|bat)$/iu.test(command),
    windowsHide: true,
  });

  if (result.error) {
    throw result.error;
  }
  if (result.status !== 0) {
    throw new Error(
      `${command} ${args.join(" ")} 执行失败（退出码 ${result.status ?? "未知"}）`,
    );
  }

  return `${result.stdout ?? ""}\n${result.stderr ?? ""}`.trim();
}

async function readWorkspaceFiles(root) {
  const fileNames = [
    "package.json",
    "backend/pom.xml",
    ".github/workflows/ci.yml",
    "backend/Dockerfile",
    "frontend/Dockerfile",
    "Dockerfile",
    "ecosystem.config.cjs",
  ];
  const contents = await Promise.all(
    fileNames.map((fileName) => readFile(path.join(root, fileName), "utf8")),
  );
  return Object.fromEntries(
    fileNames.map((fileName, index) => [fileName, contents[index]]),
  );
}

export async function main(root = workspaceRoot) {
  const issues = [];
  let files;

  try {
    files = await readWorkspaceFiles(root);
  } catch (error) {
    console.error(`读取工具链配置失败：${error.message}`);
    return 1;
  }

  const packageJson = JSON.parse(files["package.json"]);
  issues.push(
    ...validateDeclaredToolchain(packageJson, files["backend/pom.xml"]),
    ...validateRepositoryToolchainFiles({
      ci: files[".github/workflows/ci.yml"],
      backendDocker: files["backend/Dockerfile"],
      frontendDocker: files["frontend/Dockerfile"],
      rootDocker: files.Dockerfile,
      ecosystem: files["ecosystem.config.cjs"],
    }),
    ...validateCiJobsRunningToolchain(files[".github/workflows/ci.yml"]),
    ...validateOpenApiExportJvmMemory(files["backend/pom.xml"]),
  );

  try {
    const npmVersionFromUserAgent = process.env.npm_config_user_agent?.match(
      /(?:^|\s)npm\/([^\s]+)/u,
    )?.[1];
    const npmCommand = process.platform === "win32" ? "npm.cmd" : "npm";
    const npmVersion =
      npmVersionFromUserAgent ??
      runVersionCommand(npmCommand, ["--version"], root).split(/\s+/u)[0];
    const javaVersionOutput = runVersionCommand("java", ["-version"], root);

    issues.push(
      ...validateRuntimeToolchain({
        nodeVersion: process.versions.node,
        npmVersion,
        javaVersionOutput,
      }),
    );
  } catch (error) {
    issues.push(`无法执行本机工具链版本检查：${error.message}`);
  }

  if (issues.length > 0) {
    console.error("工具链校验失败：");
    for (const issue of issues) {
      console.error(`- ${issue}`);
    }
    return 1;
  }

  console.log(
    `工具链校验通过：JDK ${REQUIRED_JAVA_MAJOR}、Node.js ${REQUIRED_NODE_MAJOR}、npm ${REQUIRED_NPM_VERSION}`,
  );
  return 0;
}

if (process.argv[1] && path.resolve(process.argv[1]) === scriptPath) {
  process.exitCode = await main();
}

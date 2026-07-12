import assert from "node:assert/strict";
import { mkdtemp, mkdir, rm, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import { scanLegacy } from "./check-legacy-removed.mjs";
import {
  parseJavaMajor,
  validateCiJobsRunningToolchain,
  validateDeclaredToolchain,
  validateOpenApiExportJvmMemory,
  validateRepositoryToolchainFiles,
} from "./verify-toolchain.mjs";

test("能够解析 OpenJDK 与 Oracle Java 版本", () => {
  assert.equal(parseJavaMajor('openjdk version "25.0.3" 2026-04-21 LTS'), 25);
  assert.equal(parseJavaMajor('java version "21.0.7" 2025-04-15 LTS'), 21);
});

test("锁定 Node 24、npm 11.11.0 与 Java 25", () => {
  const issues = validateDeclaredToolchain(
    {
      packageManager: "npm@11.11.0",
      engines: { node: ">=24 <25", npm: ">=11" },
    },
    `
      <project>
        <properties>
          <java.version>25</java.version>
        </properties>
      </project>
    `,
  );

  assert.deepEqual(issues, []);
});

test("声明偏离锁定版本时返回清晰问题", () => {
  const issues = validateDeclaredToolchain(
    {
      packageManager: "npm@11.10.0",
      engines: { node: ">=22", npm: ">=10" },
    },
    "<project><properties><java.version>21</java.version></properties></project>",
  );

  assert.deepEqual(issues, [
    "packageManager 必须锁定 npm@11.11.0",
    "engines.node 必须为 >=24 <25",
    "engines.npm 必须为 >=11",
    "backend/pom.xml 的 java.version 必须为 25",
  ]);
});

test("CI 与 Docker 镜像使用锁定的 Java 25 和 Node 24", () => {
  const issues = validateRepositoryToolchainFiles({
    ci: "配置 JDK 25\njava-version: 25\n配置 JDK 25\njava-version: 25",
    backendDocker:
      "FROM maven:3.9-eclipse-temurin-25 AS build\nFROM eclipse-temurin:25-jre-alpine",
    frontendDocker: "FROM node:24-alpine AS build",
    rootDocker:
      "FROM maven:3.9-eclipse-temurin-25 AS backend-build\nFROM node:24-alpine AS frontend-build\nFROM eclipse-temurin:25-jre-alpine",
    ecosystem:
      "const REQUIRED_JAVA_MAJOR = 25;\nconst REQUIRED_NODE_MAJOR = 24;",
  });

  assert.deepEqual(issues, []);
});

test("CI 或 Docker 回退旧工具链时门禁失败", () => {
  const issues = validateRepositoryToolchainFiles({
    ci: "配置 JDK 21\njava-version: 21",
    backendDocker:
      "FROM maven:3.9-eclipse-temurin-17 AS build\nFROM eclipse-temurin:17-jre-alpine",
    frontendDocker: "FROM node:20-alpine AS build",
    rootDocker: "FROM node:20-alpine AS frontend-build",
    ecosystem:
      "const REQUIRED_JAVA_MAJOR = 21;\nconst REQUIRED_NODE_MAJOR = 20;",
  });

  assert.deepEqual(issues, [
    ".github/workflows/ci.yml 必须统一使用 JDK 25",
    "backend/Dockerfile 必须使用 JDK 25 构建与运行镜像",
    "frontend/Dockerfile 必须使用 Node 24 构建镜像",
    "根 Dockerfile 必须使用 JDK 25 与 Node 24",
    "ecosystem.config.cjs 必须声明 JDK 25 与 Node 24 LTS",
  ]);
});

test("运行工具链校验的每个 CI job 都必须配置 JDK 25", () => {
  const issues = validateCiJobsRunningToolchain(`
jobs:
  api-contract:
    steps:
      - uses: actions/setup-java@v4
        with:
          java-version: 25
      - run: npm run api:check
  frontend-quality:
    steps:
      - uses: actions/setup-node@v4
        with:
          node-version: 24.14.1
      - run: npm run verify:toolchain
  backend-quality:
    steps:
      - uses: actions/setup-java@v4
        with:
          java-version: 25
      - run: npm run verify:toolchain
  `);

  assert.deepEqual(issues, [
    "CI job frontend-quality 在运行 verify:toolchain 时必须配置 JDK 25",
  ]);
});

test("OpenAPI 导出 JVM 必须限制堆、元空间与压缩类空间", () => {
  const safePom = `
    <profile>
      <id>openapi-export</id>
      <jvmArguments>
        -Xms128m -Xmx512m
        -XX:MaxMetaspaceSize=256m
        -XX:CompressedClassSpaceSize=128m
      </jvmArguments>
    </profile>
  `;
  assert.deepEqual(validateOpenApiExportJvmMemory(safePom), []);

  const unboundedPom = `
    <profile>
      <id>openapi-export</id>
      <jvmArguments>-Dserver.port=18080</jvmArguments>
    </profile>
  `;
  assert.deepEqual(validateOpenApiExportJvmMemory(unboundedPom), [
    "backend/pom.xml 的 openapi-export JVM 必须限制为 Xms128m/Xmx512m、MaxMetaspaceSize=256m、CompressedClassSpaceSize=128m",
  ]);
});

test("遗留扫描器可在不依赖 Unix 命令的环境中发现旧模块", async (t) => {
  const fixtureRoot = await mkdtemp(path.join(os.tmpdir(), "jingxuan-legacy-"));
  t.after(() => rm(fixtureRoot, { recursive: true, force: true }));

  const legacyDir = path.join(
    fixtureRoot,
    "backend/src/main/java/com/jingxuan/modules/work/service/impl",
  );
  await mkdir(legacyDir, { recursive: true });
  await writeFile(
    path.join(legacyDir, "WorkServiceImpl.java"),
    "package com.jingxuan.modules.work.service.impl;\nclass WorkServiceImpl {}\n",
    "utf8",
  );

  const violations = await scanLegacy(fixtureRoot);

  assert.ok(
    violations.some(({ label }) => label.includes("旧 modules/work/ 仍有残留")),
  );
});

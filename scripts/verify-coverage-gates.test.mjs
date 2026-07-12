import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";
import test from "node:test";

const workspaceRoot = fileURLToPath(new URL("..", import.meta.url));

function readWorkspaceFile(relativePath) {
  return readFileSync(path.join(workspaceRoot, relativePath), "utf8");
}

test("后端 JaCoCo 在单元测试生命周期锁定可靠覆盖率基线", () => {
  const pom = readWorkspaceFile("backend/pom.xml");

  assert.match(pom, /<jacoco\.skip>\$\{skipUnitTests}<\/jacoco\.skip>/);
  assert.match(
    pom,
    /<jacoco\.coverage\.line\.minimum>0\.4100<\/jacoco\.coverage\.line\.minimum>/,
  );
  assert.match(
    pom,
    /<jacoco\.coverage\.method\.minimum>0\.4090<\/jacoco\.coverage\.method\.minimum>/,
  );
  assert.match(
    pom,
    /<jacoco\.coverage\.branch\.minimum>0\.3270<\/jacoco\.coverage\.branch\.minimum>/,
  );

  const findExecution = (id) =>
    pom.match(
      new RegExp(`<execution>\\s*<id>${id}</id>[\\s\\S]*?</execution>`),
    )?.[0];
  const prepareExecution = findExecution("prepare-unit-tests");
  const reportExecution = findExecution("report-unit-test-coverage");
  const execution = findExecution("check-unit-test-coverage");

  assert.ok(prepareExecution, "缺少 prepare-unit-tests 执行配置");
  assert.ok(reportExecution, "缺少 report-unit-test-coverage 执行配置");
  assert.ok(execution, "缺少 check-unit-test-coverage 执行配置");
  assert.match(prepareExecution, /<skip>\$\{jacoco\.skip}<\/skip>/);
  assert.match(prepareExecution, /<append>false<\/append>/);
  assert.match(reportExecution, /<skip>\$\{jacoco\.skip}<\/skip>/);
  assert.match(execution, /<phase>test<\/phase>/);
  assert.match(execution, /<goal>check<\/goal>/);
  assert.match(execution, /<skip>\$\{jacoco\.skip}<\/skip>/);

  for (const [counter, property] of [
    ["LINE", "jacoco.coverage.line.minimum"],
    ["METHOD", "jacoco.coverage.method.minimum"],
    ["BRANCH", "jacoco.coverage.branch.minimum"],
  ]) {
    const escapedProperty = property.replaceAll(".", "\\.");
    const limit = new RegExp(
      `<counter>${counter}</counter>[\\s\\S]*?<value>COVEREDRATIO</value>[\\s\\S]*?<minimum>\\$\\{${escapedProperty}\\}</minimum>`,
    );
    assert.match(execution, limit);
  }

  assert.match(
    pom,
    /最终门槛[^\n]*行[^\n]*80%[^\n]*方法[^\n]*80%[^\n]*分支[^\n]*70%/,
  );
  assert.match(
    pom,
    /<artifactId>maven-clean-plugin<\/artifactId>[\s\S]*?<include>jingxuan-backend-jacoco\.exec<\/include>/,
  );
});

test("前端 Vitest 锁定当前全量源码覆盖率基线", () => {
  const vitestConfig = readWorkspaceFile("frontend/vitest.config.ts");

  assert.match(vitestConfig, /statements:\s*22\.78/);
  assert.match(vitestConfig, /branches:\s*23\.73/);
  assert.match(vitestConfig, /functions:\s*16\.95/);
  assert.match(vitestConfig, /lines:\s*23\.85/);
  assert.match(
    vitestConfig,
    /最终门槛[^\n]*行[^\n]*80%[^\n]*函数[^\n]*80%[^\n]*分支[^\n]*70%/,
  );
});

test("根验证命令和 CI 复用原测试生命周期执行覆盖率门禁", () => {
  const workspacePackage = JSON.parse(readWorkspaceFile("package.json"));
  const ci = readWorkspaceFile(".github/workflows/ci.yml");

  assert.equal(
    workspacePackage.scripts["verify:coverage-gates"],
    "node --test scripts/verify-coverage-gates.test.mjs",
  );
  assert.equal(
    workspacePackage.scripts["frontend:test:coverage"],
    "npm --prefix frontend run test:coverage --",
  );
  assert.match(
    workspacePackage.scripts["verify:frontend"],
    /npm run verify:coverage-gates/,
  );
  assert.match(
    workspacePackage.scripts["verify:frontend"],
    /npm run frontend:test:coverage/,
  );
  assert.doesNotMatch(
    workspacePackage.scripts["verify:frontend"],
    /npm run frontend:test(?:\s|$)/,
  );

  assert.match(
    ci,
    /执行前端单元测试与覆盖率门禁[\s\S]*?npm run frontend:test:coverage/,
  );
  assert.doesNotMatch(ci, /npm --prefix frontend run test --/);
  assert.match(ci, /执行后端单元与集成验证[\s\S]*?npm run backend:verify/);
  assert.doesNotMatch(ci, /jacoco:check/);
});

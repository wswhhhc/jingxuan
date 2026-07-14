import assert from "node:assert/strict";
import test from "node:test";

import {
  GENERATED_ARTIFACT_PATHS,
  assertGeneratedArtifactsClean,
} from "./check-generated-artifacts.mjs";

test("生成产物洁净性检查覆盖 OpenAPI 与前端客户端", () => {
  assert.deepEqual(GENERATED_ARTIFACT_PATHS, [
    "openapi/jingxuan-v1-live.json",
    "openapi/jingxuan-v1.yaml",
    "frontend/src/shared/api/generated",
  ]);
});

test("生成产物洁净性检查接受无改动状态", () => {
  assert.doesNotThrow(() => assertGeneratedArtifactsClean(""));
});

test("生成产物洁净性检查拒绝未跟踪的重建文件", () => {
  assert.throws(
    () =>
      assertGeneratedArtifactsClean(
        "?? frontend/src/shared/api/generated/users/users.ts\n",
      ),
    /生成产物未提交.*\?\? frontend\/src\/shared\/api\/generated\/users\/users\.ts/s,
  );
});

test("生成产物洁净性检查拒绝已跟踪文件被删除或修改", () => {
  assert.throws(
    () =>
      assertGeneratedArtifactsClean(
        " D frontend/src/shared/api/generated/users/users.ts\n M openapi/jingxuan-v1.yaml\n",
      ),
    /生成产物未提交.* D frontend\/src\/shared\/api\/generated\/users\/users\.ts.* M openapi\/jingxuan-v1\.yaml/s,
  );
});

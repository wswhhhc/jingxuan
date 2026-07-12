import assert from "node:assert/strict";
import test from "node:test";

import {
  diffSnapshots,
  findGeneratedErrorTypeViolations,
  findGeneratedUrls,
} from "./check-generated-api.mjs";

test("生成快照差异区分新增、删除和修改文件", () => {
  const before = new Map([
    ["removed.ts", "before"],
    ["changed.ts", "before"],
    ["same.ts", "same"],
  ]);
  const after = new Map([
    ["added.ts", "after"],
    ["changed.ts", "after"],
    ["same.ts", "same"],
  ]);

  assert.deepEqual(diffSnapshots(before, after), {
    added: ["added.ts"],
    removed: ["removed.ts"],
    changed: ["changed.ts"],
  });
});

test("从 Orval 客户端中提取模板字符串 URL", () => {
  assert.deepEqual(
    findGeneratedUrls(`
      return apiRequest({url: \`/api/v1/works/\${id}\`, method: 'GET'})
      return apiRequest({url: \`/admin/users\`, method: 'GET'})
    `),
    ["/api/v1/works/${id}", "/admin/users"],
  );
});

test("生成的 Vue Query 错误类型必须包装 mutator 的运行时异常", () => {
  assert.deepEqual(
    findGeneratedErrorTypeViolations(`
      import type { ProblemDetails } from '../models'
      export const useLogin = <TError = ProblemDetails>() => undefined
    `),
    ["仍直接使用 ProblemDetails 作为 TError"],
  );
  assert.deepEqual(
    findGeneratedErrorTypeViolations(`
      import type { ErrorType } from '../../http'
      import type { ProblemDetails } from '../models'
      export const useLogin = <TError = ErrorType<ProblemDetails>>() => undefined
    `),
    [],
  );
});

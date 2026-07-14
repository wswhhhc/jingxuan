#!/usr/bin/env node

import { execFile } from "node:child_process";
import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { promisify } from "node:util";
import { fileURLToPath } from "node:url";

const execFileAsync = promisify(execFile);
const scriptPath = fileURLToPath(import.meta.url);
const workspaceRoot = path.resolve(path.dirname(scriptPath), "..");
const inputPath = path.join(
  workspaceRoot,
  "backend",
  "target",
  "openapi",
  "openapi.json",
);
const outputPath = path.join(workspaceRoot, "openapi", "jingxuan-v1-live.json");
const outputRelativePath = "openapi/jingxuan-v1-live.json";

const HTTP_METHODS = new Set([
  "delete",
  "get",
  "head",
  "options",
  "patch",
  "post",
  "put",
  "trace",
]);
const DOCUMENTATION_KEYS = new Set([
  "description",
  "example",
  "examples",
  "externalDocs",
  "operationId",
  "servers",
  "summary",
  "tags",
  "title",
]);
const NAMED_MAP_KEYS = new Set([
  "$defs",
  "callbacks",
  "content",
  "dependentSchemas",
  "encoding",
  "headers",
  "links",
  "mapping",
  "parameters",
  "pathItems",
  "paths",
  "patternProperties",
  "properties",
  "requestBodies",
  "responses",
  "schemas",
  "securitySchemes",
  "webhooks",
]);

function stableSemanticValue(value, preserveKeys = false) {
  if (Array.isArray(value)) {
    return value.map((item) => stableSemanticValue(item));
  }
  if (!value || typeof value !== "object") {
    return value;
  }

  return Object.fromEntries(
    Object.keys(value)
      .filter((key) => preserveKeys || !DOCUMENTATION_KEYS.has(key))
      .sort((left, right) => left.localeCompare(right))
      .map((key) => {
        const item = value[key];
        if (key === "security" && Array.isArray(item)) {
          return [
            key,
            item.map((requirement) => stableSemanticValue(requirement, true)),
          ];
        }
        return [
          key,
          stableSemanticValue(
            item,
            !Array.isArray(item) && NAMED_MAP_KEYS.has(key),
          ),
        ];
      }),
  );
}

function componentIdentity(ref) {
  if (typeof ref !== "string" || !ref.startsWith("#/components/")) {
    return null;
  }

  const segments = ref
    .slice(2)
    .split("/")
    .map((segment) => segment.replaceAll("~1", "/").replaceAll("~0", "~"));
  if (segments.length < 3 || segments[0] !== "components") {
    return null;
  }
  return { bucket: segments[1], name: segments[2], ref };
}

function visitComponentRefs(value, visitor) {
  if (Array.isArray(value)) {
    value.forEach((item) => visitComponentRefs(item, visitor));
    return;
  }
  if (!value || typeof value !== "object") {
    return;
  }

  if (typeof value.$ref === "string") {
    const identity = componentIdentity(value.$ref);
    if (identity) {
      visitor(identity);
    }
  }
  Object.values(value).forEach((item) => visitComponentRefs(item, visitor));
}

function visitSecurityRequirements(value, visitor) {
  if (Array.isArray(value)) {
    value.forEach((item) => visitSecurityRequirements(item, visitor));
    return;
  }
  if (!value || typeof value !== "object") {
    return;
  }

  for (const [key, item] of Object.entries(value)) {
    if (key === "security" && Array.isArray(item)) {
      for (const requirement of item) {
        if (requirement && typeof requirement === "object") {
          Object.keys(requirement).forEach(visitor);
        }
      }
    }
    visitSecurityRequirements(item, visitor);
  }
}

function selectV1Paths(paths) {
  return Object.fromEntries(
    Object.entries(paths)
      .filter(([route]) => route === "/api/v1" || route.startsWith("/api/v1/"))
      .map(([route, pathItem]) => {
        const semanticPathItem = Object.fromEntries(
          Object.entries(pathItem ?? {})
            .filter(
              ([key]) =>
                key === "$ref" ||
                key === "parameters" ||
                key.startsWith("x-") ||
                HTTP_METHODS.has(key.toLowerCase()),
            )
            .map(([key, value]) => [
              HTTP_METHODS.has(key.toLowerCase()) ? key.toLowerCase() : key,
              value,
            ]),
        );
        return [route, semanticPathItem];
      }),
  );
}

function selectReachableComponents(spec, semanticRoot) {
  const selected = {};
  const queued = [];
  const visited = new Set();

  const queueComponent = (identity) => {
    const key = `${identity.bucket}/${identity.name}`;
    if (!visited.has(key)) {
      queued.push(identity);
    }
  };

  visitComponentRefs(semanticRoot, queueComponent);
  visitSecurityRequirements(semanticRoot, (name) =>
    queueComponent({
      bucket: "securitySchemes",
      name,
      ref: `#/components/securitySchemes/${name}`,
    }),
  );

  while (queued.length > 0) {
    const identity = queued.shift();
    const key = `${identity.bucket}/${identity.name}`;
    if (visited.has(key)) {
      continue;
    }
    visited.add(key);

    const component = spec.components?.[identity.bucket]?.[identity.name];
    if (component === undefined) {
      throw new Error(`引用的 OpenAPI 组件不存在：${identity.ref}`);
    }

    const semanticComponent = stableSemanticValue(component);
    selected[identity.bucket] ??= {};
    selected[identity.bucket][identity.name] = semanticComponent;
    visitComponentRefs(semanticComponent, queueComponent);
  }

  return stableSemanticValue(selected);
}

export function extractV1SemanticSnapshot(spec) {
  if (!spec || typeof spec !== "object" || !spec.paths) {
    throw new Error("后端导出的 OpenAPI 缺少 paths");
  }

  const selectedPaths = selectV1Paths(spec.paths);
  if (Object.keys(selectedPaths).length === 0) {
    throw new Error("后端导出的 OpenAPI 不包含任何 /api/v1 路径");
  }

  const semanticRoot = {
    paths: stableSemanticValue(selectedPaths),
  };
  if (typeof spec.openapi === "string") {
    semanticRoot.openapi = spec.openapi;
  }
  if (Object.hasOwn(spec, "security")) {
    semanticRoot.security = stableSemanticValue(spec.security);
  }

  const components = selectReachableComponents(spec, semanticRoot);
  if (Object.keys(components).length > 0) {
    semanticRoot.components = components;
  }

  return stableSemanticValue(semanticRoot);
}

export function assertStoredSnapshot({ current, expected, tracked }) {
  if (current === null) {
    throw new Error(
      `缺少 ${outputRelativePath}，请先运行 npm run api:generate`,
    );
  }
  if (!tracked) {
    throw new Error(
      `${outputRelativePath} 未被 Git 跟踪，请运行 npm run api:generate 后提交该快照`,
    );
  }
  if (current !== expected) {
    throw new Error(
      "V1 实时语义契约快照已过期，请运行 npm run api:generate 并提交更新",
    );
  }
}

async function isOutputTracked() {
  try {
    await execFileAsync(
      "git",
      ["ls-files", "--error-unmatch", "--", outputRelativePath],
      { cwd: workspaceRoot, windowsHide: true },
    );
    return true;
  } catch (error) {
    if (error?.code === "ENOENT") {
      throw new Error("无法运行 git，不能确认 OpenAPI 快照是否已被版本控制");
    }
    return false;
  }
}

export async function main({ check = false } = {}) {
  const source = JSON.parse(await readFile(inputPath, "utf8"));
  const snapshot = extractV1SemanticSnapshot(source);
  const serialized = `${JSON.stringify(snapshot, null, 2)}\n`;
  const pathCount = Object.keys(snapshot.paths).length;
  const schemaCount = Object.keys(snapshot.components?.schemas ?? {}).length;

  if (check) {
    let current = null;
    try {
      current = await readFile(outputPath, "utf8");
    } catch (error) {
      if (error?.code !== "ENOENT") {
        throw error;
      }
    }
    const tracked = current === null ? false : await isOutputTracked();
    assertStoredSnapshot({ current, expected: serialized, tracked });
    console.log(
      `V1 实时语义契约校验通过：${pathCount} 个路径，${schemaCount} 个递归依赖 Schema`,
    );
    return;
  }

  await writeFile(outputPath, serialized, "utf8");
  console.log(
    `已从后端实时规格提取 ${pathCount} 个 V1 路径和 ${schemaCount} 个递归依赖 Schema`,
  );
}

if (process.argv[1] && path.resolve(process.argv[1]) === scriptPath) {
  await main({ check: process.argv.includes("--check") });
}

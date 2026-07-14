import assert from "node:assert/strict";
import test from "node:test";

import {
  assertStoredSnapshot,
  extractV1SemanticSnapshot,
} from "./prepare-openapi-v1.mjs";

test("extractV1SemanticSnapshot 保留 V1 语义契约及递归依赖组件", () => {
  const result = extractV1SemanticSnapshot({
    openapi: "3.1.0",
    info: { title: "仅用于文档", version: "1.0.0" },
    servers: [{ url: "http://localhost:18080" }],
    security: [{ bearerAuth: [] }],
    paths: {
      "/api/public/works": { get: { responses: { 200: {} } } },
      "/api/v1/users/{id}": {
        description: "路径说明不应进入快照",
        parameters: [
          {
            description: "参数说明不应进入快照",
            in: "path",
            name: "id",
            required: true,
            schema: { $ref: "#/components/schemas/UserId" },
          },
        ],
        get: {
          description: "操作说明不应进入快照",
          operationId: "getUser",
          security: [{ bearerAuth: ["users:read"] }],
          summary: "获取用户",
          tags: ["用户"],
          parameters: [
            {
              in: "query",
              name: "verbose",
              schema: { default: false, type: "boolean" },
            },
          ],
          requestBody: {
            content: {
              "application/json": {
                schema: { $ref: "#/components/schemas/CreateUser" },
              },
            },
            required: true,
          },
          responses: {
            200: {
              content: {
                "application/json": {
                  schema: { $ref: "#/components/schemas/User" },
                },
              },
              description: "成功",
            },
            404: { $ref: "#/components/responses/ProblemResponse" },
          },
        },
      },
    },
    components: {
      responses: {
        ProblemResponse: {
          content: {
            "application/problem+json": {
              schema: { $ref: "#/components/schemas/Problem" },
            },
          },
          description: "问题详情",
        },
      },
      schemas: {
        CreateUser: {
          description: "创建参数",
          properties: {
            manager: { $ref: "#/components/schemas/User" },
          },
          required: ["manager"],
          type: "object",
        },
        Problem: {
          properties: { status: { format: "int32", type: "integer" } },
          type: "object",
        },
        Unused: { type: "string" },
        User: {
          properties: {
            description: { type: "string" },
            id: { $ref: "#/components/schemas/UserId" },
            manager: { $ref: "#/components/schemas/User" },
          },
          type: "object",
        },
        UserId: { description: "用户 ID", type: "string" },
      },
      securitySchemes: {
        bearerAuth: {
          bearerFormat: "JWT",
          description: "JWT 鉴权",
          scheme: "bearer",
          type: "http",
        },
        unusedAuth: { scheme: "basic", type: "http" },
      },
    },
  });

  assert.deepEqual(result, {
    components: {
      responses: {
        ProblemResponse: {
          content: {
            "application/problem+json": {
              schema: { $ref: "#/components/schemas/Problem" },
            },
          },
        },
      },
      schemas: {
        CreateUser: {
          properties: {
            manager: { $ref: "#/components/schemas/User" },
          },
          required: ["manager"],
          type: "object",
        },
        Problem: {
          properties: { status: { format: "int32", type: "integer" } },
          type: "object",
        },
        User: {
          properties: {
            description: { type: "string" },
            id: { $ref: "#/components/schemas/UserId" },
            manager: { $ref: "#/components/schemas/User" },
          },
          type: "object",
        },
        UserId: { type: "string" },
      },
      securitySchemes: {
        bearerAuth: {
          bearerFormat: "JWT",
          scheme: "bearer",
          type: "http",
        },
      },
    },
    openapi: "3.1.0",
    paths: {
      "/api/v1/users/{id}": {
        get: {
          parameters: [
            {
              in: "query",
              name: "verbose",
              schema: { default: false, type: "boolean" },
            },
          ],
          requestBody: {
            content: {
              "application/json": {
                schema: { $ref: "#/components/schemas/CreateUser" },
              },
            },
            required: true,
          },
          responses: {
            200: {
              content: {
                "application/json": {
                  schema: { $ref: "#/components/schemas/User" },
                },
              },
            },
            404: { $ref: "#/components/responses/ProblemResponse" },
          },
          security: [{ bearerAuth: ["users:read"] }],
        },
        parameters: [
          {
            in: "path",
            name: "id",
            required: true,
            schema: { $ref: "#/components/schemas/UserId" },
          },
        ],
      },
    },
    security: [{ bearerAuth: [] }],
  });
  assert.deepEqual(Object.keys(result.components.schemas), [
    "CreateUser",
    "Problem",
    "User",
    "UserId",
  ]);
});

test("extractV1SemanticSnapshot 在导出结果没有 V1 路径时失败", () => {
  assert.throws(
    () =>
      extractV1SemanticSnapshot({
        paths: { "/api/public/works": { get: {} } },
      }),
    /不包含任何 \/api\/v1 路径/,
  );
});

test("extractV1SemanticSnapshot 在组件引用不存在时失败", () => {
  assert.throws(
    () =>
      extractV1SemanticSnapshot({
        paths: {
          "/api/v1/users": {
            get: {
              responses: {
                200: {
                  content: {
                    "application/json": {
                      schema: { $ref: "#/components/schemas/Missing" },
                    },
                  },
                },
              },
            },
          },
        },
      }),
    /引用的 OpenAPI 组件不存在.*Missing/,
  );
});

test("assertStoredSnapshot 在快照缺失时失败", () => {
  assert.throws(
    () =>
      assertStoredSnapshot({ current: null, expected: "{}\n", tracked: false }),
    /缺少 openapi\/jingxuan-v1-live\.json/,
  );
});

test("assertStoredSnapshot 在快照未被 Git 跟踪时失败", () => {
  assert.throws(
    () =>
      assertStoredSnapshot({
        current: "{}\n",
        expected: "{}\n",
        tracked: false,
      }),
    /未被 Git 跟踪/,
  );
});

test("assertStoredSnapshot 在实时语义契约漂移时失败", () => {
  assert.throws(
    () =>
      assertStoredSnapshot({
        current: "{}\n",
        expected: '{"changed":true}\n',
        tracked: true,
      }),
    /V1 实时语义契约快照已过期/,
  );
});

test("assertStoredSnapshot 接受已跟踪且与实时规格一致的快照", () => {
  assert.doesNotThrow(() =>
    assertStoredSnapshot({ current: "{}\n", expected: "{}\n", tracked: true }),
  );
});

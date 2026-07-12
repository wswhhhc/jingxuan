import assert from "node:assert/strict";
import test from "node:test";

import {
  assertOpenApiSemantics,
  assertOnlyV1Paths,
  assertOpenApiDocumentsMatch,
  normalizeOpenApi,
} from "./check-openapi-contract.mjs";

const problemResponse = () => ({
  description: "错误响应",
  content: {
    "application/problem+json": {
      schema: { $ref: "#/components/schemas/ProblemDetails" },
    },
  },
});

const LOCKED_SUCCESS_STATUSES = new Map([
  ["POST /api/v1/auth/challenges", "201"],
  ["POST /api/v1/auth/login", "200"],
  ["POST /api/v1/auth/logout", "204"],
  ["GET /api/v1/auth/me", "200"],
  ["POST /api/v1/auth/refresh", "200"],
  ["GET /api/v1/batches", "200"],
  ["GET /api/v1/classes", "200"],
  ["GET /api/v1/dictionaries/{type}", "200"],
  ["GET /api/v1/me/tasks", "200"],
  ["POST /api/v1/me/tasks/{taskId}/completion", "204"],
  ["GET /api/v1/me/works", "200"],
  ["POST /api/v1/me/works", "201"],
  ["GET /api/v1/me/works/{id}", "200"],
  ["PUT /api/v1/me/works/{id}", "204"],
  ["POST /api/v1/me/works/{id}/deletion-requests", "201"],
  ["POST /api/v1/me/works/{id}/submissions", "204"],
  ["GET /api/v1/showcase/works/{id}", "200"],
  ["GET /api/v1/tags", "200"],
  ["POST /api/v1/tags", "201"],
  ["PUT /api/v1/tags/{id}", "200"],
  ["DELETE /api/v1/tags/{id}", "204"],
  ["GET /api/v1/tags/{id}/deletion-impact", "200"],
  ["POST /api/v1/works/{id}/audit-decisions", "204"],
  ["POST /api/v1/works/{id}/comments", "201"],
  ["PUT /api/v1/works/{id}/likes", "204"],
  ["DELETE /api/v1/works/{id}/likes", "204"],
  ["POST /api/v1/works/{id}/publication", "204"],
  ["POST /api/v1/works/{id}/publication/featured", "204"],
  ["POST /api/v1/works/{id}/publication/offline", "204"],
  ["PUT /api/v1/works/{id}/scores/me", "204"],
  ["DELETE /api/v1/works/comments/{id}", "204"],
]);

const PUBLIC_OPERATION_KEYS = new Set([
  "POST /api/v1/auth/challenges",
  "POST /api/v1/auth/login",
  "POST /api/v1/auth/refresh",
  "GET /api/v1/classes",
  "GET /api/v1/dictionaries/{type}",
  "GET /api/v1/tags",
  "GET /api/v1/showcase/works/{id}",
]);

const responses = ({
  validation = false,
  protectedOperation = false,
  authenticationFailure = false,
  successStatus = "200",
} = {}) => ({
  [successStatus]: { description: "成功" },
  400: problemResponse(),
  ...(protectedOperation
    ? { 401: problemResponse(), 403: problemResponse() }
    : {}),
  ...(authenticationFailure && !protectedOperation
    ? { 401: problemResponse() }
    : {}),
  ...(validation ? { 422: problemResponse() } : {}),
  500: problemResponse(),
  default: problemResponse(),
});

function validContract() {
  const contract = {
    openapi: "3.1.0",
    servers: [{ url: "/" }],
    components: {
      securitySchemes: {
        BearerAuth: { type: "http", scheme: "bearer", bearerFormat: "JWT" },
      },
      schemas: {
        ProblemDetails: {
          type: "object",
          properties: {
            type: { type: "string", format: "uri" },
            title: { type: "string" },
            status: { type: "integer", format: "int32" },
            detail: { type: "string" },
            instance: { type: "string", format: "uri-reference" },
            code: { type: "string" },
            requestId: { type: "string" },
            fieldErrors: {
              type: "object",
              additionalProperties: { type: "string" },
            },
          },
          required: [
            "type",
            "title",
            "status",
            "detail",
            "instance",
            "code",
            "requestId",
          ],
        },
        LoginRequest: {
          type: "object",
          properties: { username: { type: "string", minLength: 1 } },
        },
        V1LoginResponse: {
          type: "object",
          properties: {
            accessToken: { type: "string" },
            tokenType: { type: "string" },
            expiresIn: { type: "integer", format: "int64" },
            user: { type: "object" },
          },
          required: ["accessToken", "tokenType", "expiresIn", "user"],
        },
        V1WorkSummary: {
          type: "object",
          properties: { id: { type: "string" } },
        },
        V1CreateWorkRequest: {
          type: "object",
          properties: {
            attachmentIds: {
              type: "array",
              items: { type: "string", pattern: "[0-9]{1,19}" },
            },
          },
        },
      },
    },
    paths: {
      "/api/v1/auth/challenges": {
        post: {
          security: [],
          requestBody: {
            content: {
              "application/json": {
                schema: { $ref: "#/components/schemas/V1ChallengeRequest" },
              },
            },
          },
          responses: {
            ...responses({ validation: true, successStatus: "201" }),
            429: problemResponse(),
            503: problemResponse(),
          },
        },
      },
      "/api/v1/auth/login": {
        post: {
          security: [],
          requestBody: {
            content: {
              "application/json": {
                schema: { $ref: "#/components/schemas/LoginRequest" },
              },
            },
          },
          responses: responses({
            validation: true,
            authenticationFailure: true,
          }),
        },
      },
      "/api/v1/auth/refresh": {
        post: {
          security: [],
          responses: responses({
            authenticationFailure: true,
          }),
        },
      },
      "/api/v1/classes": {
        get: { security: [], responses: responses() },
      },
      "/api/v1/dictionaries/{type}": {
        get: {
          security: [],
          parameters: [
            {
              name: "type",
              in: "path",
              required: true,
              schema: { type: "string", pattern: "[A-Za-z]+" },
            },
          ],
          responses: responses({ validation: true }),
        },
      },
      "/api/v1/tags": {
        get: { security: [], responses: responses() },
      },
      "/api/v1/showcase/works/{id}": {
        get: {
          security: [],
          parameters: [{ name: "id", in: "path", schema: { type: "string" } }],
          responses: responses(),
        },
      },
      "/api/v1/me/works": {
        get: {
          security: [{ BearerAuth: [] }],
          responses: responses({ protectedOperation: true }),
        },
        post: {
          security: [{ BearerAuth: [] }],
          requestBody: {
            content: {
              "application/json": { schema: { type: "object" } },
            },
          },
          responses: responses({
            validation: true,
            protectedOperation: true,
            successStatus: "201",
          }),
        },
      },
    },
  };

  for (const [operationKey, successStatus] of LOCKED_SUCCESS_STATUSES) {
    const separator = operationKey.indexOf(" ");
    const method = operationKey.slice(0, separator).toLowerCase();
    const apiPath = operationKey.slice(separator + 1);
    contract.paths[apiPath] ??= {};
    contract.paths[apiPath][method] ??= {
      security: PUBLIC_OPERATION_KEYS.has(operationKey)
        ? []
        : [{ BearerAuth: [] }],
      responses: responses({
        protectedOperation: !PUBLIC_OPERATION_KEYS.has(operationKey),
        successStatus,
      }),
    };
  }

  for (const operationKey of [
    "POST /api/v1/auth/login",
    "POST /api/v1/auth/refresh",
    "POST /api/v1/auth/logout",
  ]) {
    const separator = operationKey.indexOf(" ");
    const method = operationKey.slice(0, separator).toLowerCase();
    const apiPath = operationKey.slice(separator + 1);
    const successStatus = LOCKED_SUCCESS_STATUSES.get(operationKey);
    contract.paths[apiPath][method].responses[successStatus].headers = {
      "Set-Cookie": {
        description:
          operationKey === "POST /api/v1/auth/logout"
            ? "jingxuan_refresh; HttpOnly; SameSite=Strict; Path=/api/v1/auth; Secure=false; Max-Age=0"
            : "jingxuan_refresh; HttpOnly; SameSite=Strict; Path=/api/v1/auth; Secure=false",
        schema: { type: "string" },
      },
    };
    contract.paths[apiPath][method].responses[403] ??= problemResponse();
  }

  return contract;
}

test("规范化 OpenAPI 时递归排序对象键", () => {
  assert.deepEqual(
    normalizeOpenApi({ paths: { b: { z: 1, a: 2 }, a: {} }, openapi: "3.1.0" }),
    { openapi: "3.1.0", paths: { a: {}, b: { a: 2, z: 1 } } },
  );
});

test("契约门禁拒绝任何非 v1 路径", () => {
  assert.throws(
    () =>
      assertOnlyV1Paths(
        { paths: { "/api/v1/works": {}, "/admin/users": {} } },
        "测试契约",
      ),
    /\/admin\/users/u,
  );
});

test("规范化后语义相同的契约可通过比较", () => {
  const contract = validContract();
  const runtime = structuredClone(contract);

  assert.equal(assertOpenApiDocumentsMatch(contract, runtime).pathCount, 26);
});

test("即使运行时快照一致也拒绝 ResultVoid 假错误契约", () => {
  const invalid = validContract();
  invalid.components.schemas.ResultVoid = { type: "object" };
  invalid.paths["/api/v1/me/works"].get.responses[401] = {
    description: "错误响应",
    content: {
      "application/json": {
        schema: { $ref: "#/components/schemas/ResultVoid" },
      },
    },
  };

  assert.throws(
    () => assertOpenApiDocumentsMatch(invalid, structuredClone(invalid)),
    /application\/problem\+json|ProblemDetails/u,
  );
});

test("公开 operation 不得继承全局 Bearer", () => {
  const invalid = validContract();
  invalid.security = [{ BearerAuth: [] }];
  delete invalid.paths["/api/v1/auth/login"].post.security;

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /POST \/api\/v1\/auth\/login.*公开.*BearerAuth/u,
  );
});

test("公开登录与刷新仍必须声明凭据失败的 401 Problem Details", () => {
  const invalid = validContract();
  delete invalid.paths["/api/v1/auth/login"].post.responses[401];

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /POST \/api\/v1\/auth\/login.*401/u,
  );
});

test("refresh 与 logout 不得再声明 JSON refresh 请求体", () => {
  const invalid = validContract();
  invalid.paths["/api/v1/auth/refresh"].post.requestBody = {
    content: {
      "application/json": { schema: { type: "object" } },
    },
  };
  invalid.paths["/api/v1/auth/refresh"].post.responses[422] = problemResponse();

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /POST \/api\/v1\/auth\/refresh.*请求体/u,
  );
});

test("认证成功响应必须通过 Set-Cookie 写入或清除 refresh Cookie", () => {
  const invalid = validContract();
  delete invalid.paths["/api/v1/auth/login"].post.responses[200].headers;

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /POST \/api\/v1\/auth\/login.*Set-Cookie/u,
  );
});

test("登录响应 schema 不得暴露 refresh token 或 refresh 有效期", () => {
  const invalid = validContract();
  invalid.components.schemas.V1LoginResponse.properties.refreshToken = {
    type: "string",
  };

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /V1LoginResponse.*refreshToken/u,
  );
});

test("契约不得保留 JSON refresh 请求 DTO", () => {
  const invalid = validContract();
  invalid.components.schemas.V1RefreshRequest = {
    type: "object",
    properties: { refreshToken: { type: "string" } },
  };

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /V1RefreshRequest/u,
  );
});

test("登录、刷新和注销必须声明 Origin 拒绝的 403 Problem Details", () => {
  const invalid = validContract();
  delete invalid.paths["/api/v1/auth/login"].post.responses[403];

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /POST \/api\/v1\/auth\/login.*403/u,
  );
});

test("refresh 与 logout 不得保留 JSON 校验产生的 422", () => {
  const invalid = validContract();
  invalid.paths["/api/v1/auth/refresh"].post.responses[422] = problemResponse();

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /POST \/api\/v1\/auth\/refresh.*422/u,
  );
});

test("V1LoginResponse 的四个 access session 字段必须为 required", () => {
  const invalid = validContract();
  invalid.components.schemas.V1LoginResponse.required = [
    "tokenType",
    "expiresIn",
    "user",
  ];

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /V1LoginResponse.required.*accessToken/u,
  );
});

test("logout 的 Set-Cookie 必须声明 Max-Age=0", () => {
  const invalid = validContract();
  invalid.paths["/api/v1/auth/logout"].post.responses[204].headers[
    "Set-Cookie"
  ].description =
    "jingxuan_refresh; HttpOnly; SameSite=Strict; Path=/api/v1/auth; Secure=false";

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /POST \/api\/v1\/auth\/logout.*Max-Age=0/u,
  );
});

test("受保护 operation 必须声明 Bearer 与 401/403 Problem Details", () => {
  const invalid = validContract();
  const operation = invalid.paths["/api/v1/me/works"].get;
  operation.security = [];
  delete operation.responses[403];

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /GET \/api\/v1\/me\/works.*BearerAuth|401\/403/u,
  );
});

test("有请求体或校验约束的 operation 必须声明 422", () => {
  const invalid = validContract();
  delete invalid.paths["/api/v1/auth/login"].post.responses[422];

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /POST \/api\/v1\/auth\/login.*422/u,
  );
});

test("每个 v1 operation 必须且只能声明锁定的成功状态", () => {
  const invalid = validContract();
  const logout = invalid.paths["/api/v1/auth/logout"].post;
  delete logout.responses[204];
  logout.responses[200] = { description: "错误回退" };

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /POST \/api\/v1\/auth\/logout.*204.*200/u,
  );
});

test("契约不得固化构建机 localhost server", () => {
  const invalid = validContract();
  invalid.servers = [{ url: "http://localhost:18080" }];

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /server.*相对/u,
  );
});

test("所有雪花 ID schema 必须是 string，禁止 integer int64", () => {
  const invalid = validContract();
  invalid.components.schemas.ScoreSubmitRequest = {
    type: "object",
    properties: {
      workId: { type: "integer", format: "int64" },
    },
  };
  invalid.components.schemas.WorkMemberDTO = {
    type: "object",
    properties: {
      id: { type: "integer", format: "int64" },
      studentId: { type: "integer", format: "int64" },
    },
  };

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /ScoreSubmitRequest\.workId|WorkMemberDTO\.(?:id|studentId)/u,
  );
});

test("复数雪花 ID 必须保持数组且数组元素为 string", () => {
  const invalid = validContract();
  invalid.components.schemas.V1CreateWorkRequest.properties.attachmentIds = {
    type: "string",
    items: { type: "string" },
  };

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /V1CreateWorkRequest\.attachmentIds/u,
  );
});

test("ProblemDetails 必须保留 RFC 字段与平台扩展字段", () => {
  const invalid = validContract();
  invalid.components.schemas.ProblemDetails = { type: "object" };

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /ProblemDetails/u,
  );
});

test("ID 门禁也扫描 operation 的内联响应 schema", () => {
  const invalid = validContract();
  invalid.paths["/api/v1/me/works"].get.responses[200] = {
    description: "成功",
    content: {
      "application/json": {
        schema: {
          type: "object",
          properties: {
            ownerId: { type: "integer", format: "int64" },
          },
        },
      },
    },
  };

  assert.throws(
    () => assertOpenApiSemantics(invalid, "测试契约"),
    /response application\/json\.ownerId/u,
  );
});

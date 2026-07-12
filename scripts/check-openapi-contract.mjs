import { createHash } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const workspaceRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
const requireFromFrontend = createRequire(
  path.join(workspaceRoot, "frontend", "package.json"),
);
const { parse: parseYaml, stringify: stringifyYaml } =
  requireFromFrontend("yaml");

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
const PUBLIC_OPERATIONS = new Set([
  "POST /api/v1/auth/challenges",
  "POST /api/v1/auth/email-verifications",
  "POST /api/v1/auth/login",
  "POST /api/v1/auth/registrations",
  "POST /api/v1/auth/refresh",
  "GET /api/v1/classes",
  "GET /api/v1/dictionaries/{type}",
  "GET /api/v1/tags",
  "GET /api/v1/showcase/works/{id}",
]);
const AUTHENTICATION_FAILURE_OPERATIONS = new Set([
  "POST /api/v1/auth/login",
  "POST /api/v1/auth/refresh",
]);
const REFRESH_COOKIE_OPERATIONS = new Set([
  "POST /api/v1/auth/login",
  "POST /api/v1/auth/refresh",
  "POST /api/v1/auth/logout",
]);
const NO_REFRESH_BODY_OPERATIONS = new Set([
  "POST /api/v1/auth/refresh",
  "POST /api/v1/auth/logout",
]);
const EXPECTED_SUCCESS_STATUSES = new Map([
  ["POST /api/v1/auth/challenges", "201"],
  ["POST /api/v1/auth/email-verifications", "204"],
  ["POST /api/v1/auth/login", "200"],
  ["POST /api/v1/auth/registrations", "201"],
  ["POST /api/v1/auth/logout", "204"],
  ["POST /api/v1/auth/logout-all", "204"],
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
  ["GET /api/v1/works/{id}/comments", "200"],
  ["GET /api/v1/works/{id}/comments/{commentId}/replies", "200"],
  ["PUT /api/v1/works/{id}/likes", "204"],
  ["DELETE /api/v1/works/{id}/likes", "204"],
  ["POST /api/v1/works/{id}/publication", "204"],
  ["POST /api/v1/works/{id}/publication/featured", "204"],
  ["POST /api/v1/works/{id}/publication/offline", "204"],
  ["PUT /api/v1/works/{id}/scores/me", "204"],
  ["POST /api/v1/users/{id}/approval-decisions", "204"],
  ["GET /api/v1/users/{id}/deletion-impact", "200"],
  ["DELETE /api/v1/users/{id}", "204"],
  ["DELETE /api/v1/works/comments/{id}", "204"],
]);
const ERROR_RESPONSE = /^(?:4\d\d|5\d\d|default)$/u;
const ABSOLUTE_SERVER = /^(?:[a-z][a-z\d+.-]*:|\/\/)/iu;
const VALIDATION_KEYWORDS = [
  "const",
  "enum",
  "exclusiveMaximum",
  "exclusiveMinimum",
  "maxItems",
  "maxLength",
  "maximum",
  "minItems",
  "minLength",
  "minimum",
  "multipleOf",
  "pattern",
];

export const defaultContractPath = path.join(
  workspaceRoot,
  "openapi",
  "jingxuan-v1.yaml",
);
export const defaultRuntimePath = path.join(
  workspaceRoot,
  "backend",
  "target",
  "openapi",
  "openapi.json",
);

function isObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function operationEntries(document) {
  return Object.entries(document.paths ?? {}).flatMap(([apiPath, pathItem]) =>
    Object.entries(pathItem ?? {})
      .filter(
        ([method, operation]) =>
          HTTP_METHODS.has(method.toLowerCase()) && isObject(operation),
      )
      .map(([method, operation]) => ({
        apiPath,
        method: method.toUpperCase(),
        operation,
        operationKey: `${method.toUpperCase()} ${apiPath}`,
      })),
  );
}

function resolveRef(document, value) {
  if (!isObject(value) || typeof value.$ref !== "string") {
    return value;
  }
  if (!value.$ref.startsWith("#/")) {
    return value;
  }

  return value.$ref
    .slice(2)
    .split("/")
    .map((segment) => segment.replaceAll("~1", "/").replaceAll("~0", "~"))
    .reduce((current, segment) => current?.[segment], document);
}

function effectiveSecurity(document, operation) {
  const security = operation.security ?? document.security ?? [];
  return Array.isArray(security) ? security : [];
}

function hasBearerAuth(security) {
  return security.some(
    (requirement) =>
      isObject(requirement) &&
      Object.prototype.hasOwnProperty.call(requirement, "BearerAuth"),
  );
}

function assertProblemResponse(document, response, operationKey, status) {
  const resolvedResponse = resolveRef(document, response);
  const media = resolvedResponse?.content?.["application/problem+json"];
  const schemaRef = media?.schema?.$ref;
  if (schemaRef !== "#/components/schemas/ProblemDetails") {
    throw new Error(
      `${operationKey} 的 ${status} 错误响应必须使用 application/problem+json 并引用 ProblemDetails`,
    );
  }
}

function schemaHasValidation(document, schema, seen = new Set()) {
  const resolved = resolveRef(document, schema);
  if (!isObject(resolved) || seen.has(resolved)) {
    return false;
  }
  seen.add(resolved);

  if (VALIDATION_KEYWORDS.some((keyword) => resolved[keyword] !== undefined)) {
    return true;
  }
  if (isObject(resolved.properties)) {
    if (
      Object.values(resolved.properties).some((property) =>
        schemaHasValidation(document, property, seen),
      )
    ) {
      return true;
    }
  }
  if (schemaHasValidation(document, resolved.items, seen)) {
    return true;
  }
  return ["allOf", "anyOf", "oneOf"].some(
    (keyword) =>
      Array.isArray(resolved[keyword]) &&
      resolved[keyword].some((part) =>
        schemaHasValidation(document, part, seen),
      ),
  );
}

function operationNeedsValidationResponse(document, operation) {
  if (operation.requestBody !== undefined) {
    return true;
  }
  return (operation.parameters ?? []).some((parameter) => {
    const resolvedParameter = resolveRef(document, parameter);
    return schemaHasValidation(document, resolvedParameter?.schema);
  });
}

function isIdentifierName(name) {
  return (
    name === "id" ||
    name.endsWith("Id") ||
    name.endsWith("Ids") ||
    /(?:^|[_-])ids?$/iu.test(name)
  );
}

function schemaTypes(schema) {
  const type = schema?.type;
  if (Array.isArray(type)) {
    return type;
  }
  return typeof type === "string" ? [type] : [];
}

function identifierSchemaIsString(document, schema, plural) {
  const resolved = resolveRef(document, schema);
  if (!isObject(resolved) || resolved.format === "int64") {
    return false;
  }
  const types = schemaTypes(resolved);
  if (plural) {
    return (
      types.includes("array") &&
      identifierSchemaIsString(document, resolved.items, false)
    );
  }
  return types.includes("string") && !types.includes("integer");
}

function assertProblemDetailsSchema(document, label) {
  const schema = resolveRef(
    document,
    document.components?.schemas?.ProblemDetails,
  );
  const properties = schema?.properties;
  const required = new Set(schema?.required ?? []);
  const expected = new Map([
    ["type", { type: "string", format: "uri", required: true }],
    ["title", { type: "string", required: true }],
    ["status", { type: "integer", required: true }],
    ["detail", { type: "string", required: true }],
    ["instance", { type: "string", format: "uri-reference", required: true }],
    ["code", { type: "string", required: true }],
    ["requestId", { type: "string", required: true }],
    ["fieldErrors", { type: "object", required: false }],
  ]);

  if (
    !isObject(schema) ||
    !schemaTypes(schema).includes("object") ||
    !isObject(properties)
  ) {
    throw new Error(
      `${label} 的 ProblemDetails 必须是包含标准字段的 object schema`,
    );
  }

  for (const [field, rule] of expected) {
    const property = resolveRef(document, properties[field]);
    if (!isObject(property) || !schemaTypes(property).includes(rule.type)) {
      throw new Error(
        `${label} 的 ProblemDetails.${field} 类型必须为 ${rule.type}`,
      );
    }
    if (rule.format && property.format !== rule.format) {
      throw new Error(
        `${label} 的 ProblemDetails.${field} format 必须为 ${rule.format}`,
      );
    }
    if (rule.required && !required.has(field)) {
      throw new Error(`${label} 的 ProblemDetails.required 必须包含 ${field}`);
    }
  }

  const fieldErrorValue = resolveRef(
    document,
    properties.fieldErrors.additionalProperties,
  );
  if (
    !isObject(fieldErrorValue) ||
    !schemaTypes(fieldErrorValue).includes("string")
  ) {
    throw new Error(`${label} 的 ProblemDetails.fieldErrors 值必须为 string`);
  }
}

function identifierViolations(document) {
  const violations = [];
  const seen = new Set();

  function visitSchema(schema, trail) {
    const resolved = resolveRef(document, schema);
    if (!isObject(resolved) || seen.has(resolved)) {
      return;
    }
    seen.add(resolved);

    if (isObject(resolved.properties)) {
      for (const [name, property] of Object.entries(resolved.properties)) {
        const propertyTrail = `${trail}.${name}`;
        if (
          isIdentifierName(name) &&
          !identifierSchemaIsString(document, property, name.endsWith("Ids"))
        ) {
          violations.push(propertyTrail);
        }
        visitSchema(property, propertyTrail);
      }
    }
    visitSchema(resolved.items, `${trail}[]`);
    for (const keyword of ["allOf", "anyOf", "oneOf"]) {
      for (const [index, part] of (resolved[keyword] ?? []).entries()) {
        visitSchema(part, `${trail}.${keyword}[${index}]`);
      }
    }
    if (isObject(resolved.additionalProperties)) {
      visitSchema(resolved.additionalProperties, `${trail}.*`);
    }
  }

  for (const [name, schema] of Object.entries(
    document.components?.schemas ?? {},
  )) {
    visitSchema(schema, name);
  }
  for (const { operationKey, operation } of operationEntries(document)) {
    for (const parameter of operation.parameters ?? []) {
      const resolvedParameter = resolveRef(document, parameter);
      const name = resolvedParameter?.name;
      if (
        typeof name === "string" &&
        isIdentifierName(name) &&
        !identifierSchemaIsString(
          document,
          resolvedParameter.schema,
          name.endsWith("Ids"),
        )
      ) {
        violations.push(`${operationKey} parameter ${name}`);
      }
      visitSchema(
        resolvedParameter?.schema,
        `${operationKey} parameter ${name ?? "unknown"}`,
      );
    }
    const requestBody = resolveRef(document, operation.requestBody);
    for (const [mediaType, media] of Object.entries(
      requestBody?.content ?? {},
    )) {
      visitSchema(media?.schema, `${operationKey} request ${mediaType}`);
    }
    for (const response of Object.values(operation.responses ?? {})) {
      const resolvedResponse = resolveRef(document, response);
      for (const [mediaType, media] of Object.entries(
        resolvedResponse?.content ?? {},
      )) {
        visitSchema(media?.schema, `${operationKey} response ${mediaType}`);
      }
    }
  }
  return violations;
}

export function assertOpenApiSemantics(document, label) {
  assertOnlyV1Paths(document, label);
  const operations = operationEntries(document);
  if (operations.length === 0) {
    throw new Error(`${label} 没有任何 v1 operation`);
  }

  for (const server of document.servers ?? []) {
    if (
      typeof server?.url !== "string" ||
      server.url.length === 0 ||
      ABSOLUTE_SERVER.test(server.url)
    ) {
      throw new Error(`${label} 的 server 必须省略或使用相对同源 URL`);
    }
  }

  const bearerScheme = document.components?.securitySchemes?.BearerAuth;
  if (
    !isObject(bearerScheme) ||
    bearerScheme.type !== "http" ||
    bearerScheme.scheme !== "bearer"
  ) {
    throw new Error(`${label} 缺少 HTTP BearerAuth security scheme`);
  }
  assertProblemDetailsSchema(document, label);

  const loginResponse = resolveRef(
    document,
    document.components?.schemas?.V1LoginResponse,
  );
  if (!isObject(loginResponse) || !isObject(loginResponse.properties)) {
    throw new Error(`${label} 缺少 V1LoginResponse object schema`);
  }
  for (const forbiddenField of ["refreshToken", "refreshExpiresIn"]) {
    if (
      Object.prototype.hasOwnProperty.call(
        loginResponse.properties,
        forbiddenField,
      )
    ) {
      throw new Error(`${label} 的 V1LoginResponse 不得暴露 ${forbiddenField}`);
    }
  }
  const requiredLoginFields = new Set(loginResponse.required ?? []);
  for (const requiredField of [
    "accessToken",
    "tokenType",
    "expiresIn",
    "user",
  ]) {
    if (
      !Object.prototype.hasOwnProperty.call(
        loginResponse.properties,
        requiredField,
      ) ||
      !requiredLoginFields.has(requiredField)
    ) {
      throw new Error(
        `${label} 的 V1LoginResponse.required 必须包含 ${requiredField}`,
      );
    }
  }
  if (document.components?.schemas?.V1RefreshRequest !== undefined) {
    throw new Error(`${label} 不得保留 JSON refresh DTO V1RefreshRequest`);
  }

  const presentOperations = new Set(
    operations.map(({ operationKey }) => operationKey),
  );
  const missingPublicOperations = [...PUBLIC_OPERATIONS].filter(
    (operationKey) => !presentOperations.has(operationKey),
  );
  if (missingPublicOperations.length > 0) {
    throw new Error(
      `${label} 缺少已锁定公开 operation：${missingPublicOperations.join(", ")}`,
    );
  }

  const missingSuccessOperations = [...EXPECTED_SUCCESS_STATUSES.keys()].filter(
    (operationKey) => !presentOperations.has(operationKey),
  );
  const unclassifiedOperations = [...presentOperations].filter(
    (operationKey) => !EXPECTED_SUCCESS_STATUSES.has(operationKey),
  );
  if (
    missingSuccessOperations.length > 0 ||
    unclassifiedOperations.length > 0
  ) {
    throw new Error(
      [
        missingSuccessOperations.length > 0
          ? `${label} 缺少锁定 operation：${missingSuccessOperations.join(", ")}`
          : null,
        unclassifiedOperations.length > 0
          ? `${label} 存在未声明成功状态的 operation：${unclassifiedOperations.join(", ")}`
          : null,
      ]
        .filter(Boolean)
        .join("\n"),
    );
  }

  for (const { operationKey, operation } of operations) {
    if (
      NO_REFRESH_BODY_OPERATIONS.has(operationKey) &&
      operation.requestBody !== undefined
    ) {
      throw new Error(`${operationKey} 不得声明 JSON refresh 请求体`);
    }
    if (
      NO_REFRESH_BODY_OPERATIONS.has(operationKey) &&
      operation.responses?.["422"] !== undefined
    ) {
      throw new Error(`${operationKey} 不得声明 JSON 请求校验产生的 422`);
    }

    const security = effectiveSecurity(document, operation);
    const isPublic = PUBLIC_OPERATIONS.has(operationKey);
    if (isPublic && security.length > 0) {
      throw new Error(`${operationKey} 是公开 operation，不得继承 BearerAuth`);
    }
    if (!isPublic && !hasBearerAuth(security)) {
      throw new Error(
        `${operationKey} 是受保护 operation，必须声明 BearerAuth`,
      );
    }

    const expectedSuccessStatus = EXPECTED_SUCCESS_STATUSES.get(operationKey);
    const actualSuccessStatuses = Object.keys(operation.responses ?? {}).filter(
      (status) => /^2\d{2}$/u.test(status),
    );
    if (
      actualSuccessStatuses.length !== 1 ||
      actualSuccessStatuses[0] !== expectedSuccessStatus
    ) {
      throw new Error(
        `${operationKey} 成功状态必须且只能为 ${expectedSuccessStatus}，实际为 ${actualSuccessStatuses.join(", ") || "缺失"}`,
      );
    }
    const successResponse = resolveRef(
      document,
      operation.responses?.[expectedSuccessStatus],
    );
    if (
      expectedSuccessStatus === "204" &&
      isObject(successResponse?.content) &&
      Object.keys(successResponse.content).length > 0
    ) {
      throw new Error(`${operationKey} 的 204 响应不得声明响应体`);
    }
    if (REFRESH_COOKIE_OPERATIONS.has(operationKey)) {
      const setCookieEntry = Object.entries(
        successResponse?.headers ?? {},
      ).find(([name]) => name.toLowerCase() === "set-cookie");
      const setCookieHeader = resolveRef(document, setCookieEntry?.[1]);
      const setCookieSchema = resolveRef(document, setCookieHeader?.schema);
      const description = setCookieHeader?.description ?? "";
      if (
        !isObject(setCookieHeader) ||
        !schemaTypes(setCookieSchema).includes("string") ||
        !/jingxuan_refresh/iu.test(description) ||
        !/HttpOnly/iu.test(description) ||
        !/SameSite=Strict/iu.test(description) ||
        !/Path=\/api\/v1\/auth/iu.test(description) ||
        !/Secure=false/iu.test(description)
      ) {
        throw new Error(
          `${operationKey} 的成功响应必须声明 Set-Cookie，并锁定 jingxuan_refresh、HttpOnly、SameSite=Strict、Path=/api/v1/auth、Secure=false`,
        );
      }
      if (
        operationKey === "POST /api/v1/auth/logout" &&
        !/Max-Age=0/iu.test(description)
      ) {
        throw new Error(
          `${operationKey} 的 Set-Cookie 必须声明 Max-Age=0 以清除 refresh Cookie`,
        );
      }
    }

    const requiredErrors = ["400", "500", "default"];
    if (!isPublic) {
      requiredErrors.push("401", "403");
    } else if (AUTHENTICATION_FAILURE_OPERATIONS.has(operationKey)) {
      requiredErrors.push("401");
    }
    if (REFRESH_COOKIE_OPERATIONS.has(operationKey)) {
      requiredErrors.push("403");
    }
    if (operationNeedsValidationResponse(document, operation)) {
      requiredErrors.push("422");
    }
    for (const status of requiredErrors) {
      if (!operation.responses?.[status]) {
        throw new Error(`${operationKey} 缺少 ${status} 错误响应声明`);
      }
    }
    for (const [status, response] of Object.entries(
      operation.responses ?? {},
    )) {
      if (ERROR_RESPONSE.test(status)) {
        assertProblemResponse(document, response, operationKey, status);
      }
    }
  }

  const idViolations = identifierViolations(document);
  if (idViolations.length > 0) {
    throw new Error(
      `${label} 的雪花 ID 必须声明为 string，禁止 integer/int64：${idViolations.join(", ")}`,
    );
  }

  return { operationCount: operations.length };
}

export function normalizeOpenApi(value) {
  if (Array.isArray(value)) {
    return value.map(normalizeOpenApi);
  }
  if (!isObject(value)) {
    return value;
  }

  return Object.fromEntries(
    Object.keys(value)
      .sort((left, right) => left.localeCompare(right, "en"))
      .map((key) => [key, normalizeOpenApi(value[key])]),
  );
}

export function assertOnlyV1Paths(document, label) {
  if (!isObject(document) || !isObject(document.paths)) {
    throw new Error(`${label} 缺少 OpenAPI paths 对象`);
  }

  const paths = Object.keys(document.paths);
  if (paths.length === 0) {
    throw new Error(`${label} 没有任何 API 路径`);
  }

  const legacyPaths = paths.filter(
    (apiPath) => apiPath !== "/api/v1" && !apiPath.startsWith("/api/v1/"),
  );
  if (legacyPaths.length > 0) {
    throw new Error(
      `${label} 包含非 /api/v1/** 路径：${legacyPaths.sort().join(", ")}`,
    );
  }

  return paths;
}

function digest(document) {
  return createHash("sha256")
    .update(JSON.stringify(normalizeOpenApi(document)))
    .digest("hex");
}

function difference(left, right) {
  const rightSet = new Set(right);
  return left.filter((item) => !rightSet.has(item)).sort();
}

export function assertOpenApiDocumentsMatch(contract, runtime) {
  const contractPaths = assertOnlyV1Paths(contract, "已提交 YAML 契约");
  const runtimePaths = assertOnlyV1Paths(runtime, "运行时 Springdoc 文档");
  assertOpenApiSemantics(contract, "已提交 YAML 契约");
  assertOpenApiSemantics(runtime, "运行时 Springdoc 文档");
  const normalizedContract = normalizeOpenApi(contract);
  const normalizedRuntime = normalizeOpenApi(runtime);
  const contractJson = JSON.stringify(normalizedContract);
  const runtimeJson = JSON.stringify(normalizedRuntime);

  if (contractJson === runtimeJson) {
    return {
      hash: digest(normalizedContract),
      pathCount: contractPaths.length,
    };
  }

  const onlyInContract = difference(contractPaths, runtimePaths);
  const onlyAtRuntime = difference(runtimePaths, contractPaths);
  throw new Error(
    [
      "已提交 YAML 契约与运行时 Springdoc 文档不一致",
      `YAML SHA-256: ${digest(normalizedContract)}`,
      `运行时 SHA-256: ${digest(normalizedRuntime)}`,
      onlyInContract.length > 0
        ? `仅 YAML 存在的路径: ${onlyInContract.join(", ")}`
        : null,
      onlyAtRuntime.length > 0
        ? `仅运行时存在的路径: ${onlyAtRuntime.join(", ")}`
        : null,
    ]
      .filter(Boolean)
      .join("\n"),
  );
}

async function readContract(contractPath) {
  return parseYaml(await readFile(contractPath, "utf8"));
}

async function readRuntime(runtimePath) {
  return JSON.parse(await readFile(runtimePath, "utf8"));
}

export async function checkOpenApiContract({
  contractPath = defaultContractPath,
  runtimePath = defaultRuntimePath,
} = {}) {
  const [contract, runtime] = await Promise.all([
    readContract(contractPath),
    readRuntime(runtimePath),
  ]);
  return assertOpenApiDocumentsMatch(contract, runtime);
}

export async function updateOpenApiContract({
  contractPath = defaultContractPath,
  runtimePath = defaultRuntimePath,
} = {}) {
  const runtime = await readRuntime(runtimePath);
  const paths = assertOnlyV1Paths(runtime, "运行时 Springdoc 文档");
  assertOpenApiSemantics(runtime, "运行时 Springdoc 文档");
  const normalizedRuntime = normalizeOpenApi(runtime);
  const yaml = stringifyYaml(normalizedRuntime, { lineWidth: 0 });

  await mkdir(path.dirname(contractPath), { recursive: true });
  await writeFile(contractPath, yaml, "utf8");
  return { hash: digest(normalizedRuntime), pathCount: paths.length };
}

async function main() {
  const writeMode = process.argv.includes("--write");
  const result = writeMode
    ? await updateOpenApiContract()
    : await checkOpenApiContract();
  console.log(
    `${writeMode ? "OpenAPI YAML 契约已更新" : "OpenAPI 契约一致性校验通过"}：${result.pathCount} 个 v1 路径，SHA-256 ${result.hash}`,
  );
}

const isMain =
  process.argv[1] &&
  pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url;

if (isMain) {
  try {
    await main();
  } catch (error) {
    console.error(error instanceof Error ? error.message : error);
    process.exitCode = 1;
  }
}

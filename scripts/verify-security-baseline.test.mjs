import assert from "node:assert/strict";
import test from "node:test";

import {
  validateComposeProxyBoundary,
  validateEnvExample,
} from "./verify-security-baseline.mjs";

const validEnvExample = `
SPRING_PROFILES_ACTIVE=dev
DB_HOST=localhost
DB_PORT=3306
DB_NAME=jingxuan
DB_USER=jingxuan
DB_ROOT_PASSWORD=
DB_LEGACY_ROOT_PASSWORD=
DB_PASSWORD=
REDIS_HOST=localhost
REDIS_PORT=6379
JINGXUAN_UPLOAD_PATH=./uploads
JINGXUAN_SECURITY_TRUSTED_PROXY_CIDRS=127.0.0.1/32,::1/128
JINGXUAN_DOCKER_TRUSTED_PROXY_CIDRS=127.0.0.1/32,::1/128,172.31.250.2/32
JWT_EXPIRATION_MS=86400000
JWT_SECRET=
# JWT 密钥在模板中必须为空
DEEPSEEK_API_KEY=
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=
`;

const validComposeProxyBoundary = `
services:
  backend:
    environment:
      JINGXUAN_SECURITY_TRUSTED_PROXY_CIDRS: "\${JINGXUAN_DOCKER_TRUSTED_PROXY_CIDRS:-127.0.0.1/32,::1/128,172.31.250.2/32}"
    ports:
      - "127.0.0.1:8080:8080"
    networks:
      proxy:
        ipv4_address: 172.31.250.3
  nginx:
    networks:
      proxy:
        ipv4_address: 172.31.250.2
networks:
  proxy:
    ipam:
      config:
        - subnet: 172.31.250.0/29
`;

test("完整的环境变量模板通过基线校验", () => {
  assert.deepEqual(validateEnvExample(validEnvExample), []);
});

test("删除任一必填键时校验失败", () => {
  const withoutJwtSecret = validEnvExample.replace("JWT_SECRET=\n", "");

  assert.ok(
    validateEnvExample(withoutJwtSecret).includes(
      ".env.example 缺少必填键 JWT_SECRET",
    ),
  );
});

test("敏感键在模板中必须保持为空", () => {
  const withPassword = validEnvExample.replace(
    "DB_PASSWORD=\n",
    "DB_PASSWORD=not-a-real-password\n",
  );

  assert.ok(
    validateEnvExample(withPassword).includes(
      ".env.example 中的 DB_PASSWORD 必须保持为空",
    ),
  );
});

test("旧卷迁移 root 密码在模板中必须存在且保持为空", () => {
  const withoutLegacyRootPassword = validEnvExample.replace(
    "DB_LEGACY_ROOT_PASSWORD=\n",
    "",
  );
  const withLegacyRootPassword = validEnvExample.replace(
    "DB_LEGACY_ROOT_PASSWORD=\n",
    "DB_LEGACY_ROOT_PASSWORD=not-a-real-legacy-password\n",
  );

  assert.ok(
    validateEnvExample(withoutLegacyRootPassword).includes(
      ".env.example 缺少必填键 DB_LEGACY_ROOT_PASSWORD",
    ),
  );
  assert.ok(
    validateEnvExample(withLegacyRootPassword).includes(
      ".env.example 中的 DB_LEGACY_ROOT_PASSWORD 必须保持为空",
    ),
  );
});

test("Docker 仅信任固定 Nginx 地址且后端端口只绑定回环地址", () => {
  assert.deepEqual(validateComposeProxyBoundary(validComposeProxyBoundary), []);
});

test("Docker 信任整个容器网段时校验失败", () => {
  const overbroadCompose = validComposeProxyBoundary.replace(
    "172.31.250.2/32",
    "172.16.0.0/12",
  );

  assert.ok(
    validateComposeProxyBoundary(overbroadCompose).includes(
      "docker-compose.yml 不得信任整个 Docker 私网网段",
    ),
  );
});

test("Docker 后端端口暴露到所有宿主机接口时校验失败", () => {
  const publiclyExposedCompose = validComposeProxyBoundary.replace(
    '"127.0.0.1:8080:8080"',
    '"8080:8080"',
  );

  assert.ok(
    validateComposeProxyBoundary(publiclyExposedCompose).includes(
      "docker-compose.yml 的后端 8080 端口必须仅绑定 127.0.0.1",
    ),
  );
});

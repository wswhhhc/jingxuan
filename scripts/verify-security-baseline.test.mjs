import assert from "node:assert/strict";
import test from "node:test";

import { validateEnvExample } from "./verify-security-baseline.mjs";

const validEnvExample = `
SPRING_PROFILES_ACTIVE=dev
DB_HOST=localhost
DB_PORT=3306
DB_NAME=jingxuan
DB_USER=root
DB_PASSWORD=
REDIS_HOST=localhost
REDIS_PORT=6379
JINGXUAN_UPLOAD_PATH=./uploads
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

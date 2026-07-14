import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import {
  access,
  chmod,
  mkdtemp,
  mkdir,
  readFile,
  readdir,
  rm,
  writeFile,
} from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const repositoryRoot = fileURLToPath(new URL("../", import.meta.url));
const smokeScript = path.join(repositoryRoot, "scripts", "smoke-test.sh");

async function readRepositoryFile(relativePath) {
  return readFile(path.join(repositoryRoot, relativePath), "utf8");
}

function extractWorkflowJob(workflow, jobName) {
  const jobStart = workflow.indexOf(`  ${jobName}:`);
  assert.notEqual(jobStart, -1, `CI 缺少独立 ${jobName} job`);

  const nextJob = workflow.slice(jobStart + 1).search(/^  [a-z0-9-]+:\s*$/mu);
  return nextJob === -1
    ? workflow.slice(jobStart)
    : workflow.slice(jobStart, jobStart + 1 + nextJob);
}

const snowflakeDraftId = "1912345678901234567";
const nodeRuntimeProbe = spawnSync("bash", ["-lc", "command -v node"], {
  encoding: "utf8",
});
const nodeExecutable =
  nodeRuntimeProbe.status === 0 ? nodeRuntimeProbe.stdout.trim() : "node";
const pythonRuntimeProbe = spawnSync("bash", ["-lc", "command -v python3"], {
  encoding: "utf8",
});
const pythonExecutable =
  pythonRuntimeProbe.status === 0 ? pythonRuntimeProbe.stdout.trim() : null;

function toBashPath(value) {
  const normalized = value.replaceAll("\\", "/");
  const drivePath = normalized.match(/^([A-Za-z]):\/(.*)$/u);
  return drivePath
    ? `/mnt/${drivePath[1].toLowerCase()}/${drivePath[2]}`
    : normalized;
}

function shellQuote(value) {
  return `'${String(value).replaceAll("'", `'"'"'`)}'`;
}

const runtimeGuardScript = String.raw`#!/usr/bin/env bash
set -euo pipefail

for argument in "$@"; do
  while IFS= read -r secret || [[ -n "$secret" ]]; do
    [[ -n "$secret" ]] || continue
    if [[ "$argument" == *"$secret"* ]]; then
      printf '%s\n' 'runtime argv-secret-detected' >> "$SMOKE_ARGV_AUDIT_LOG"
      exit 97
    fi
  done < "$SMOKE_SECRET_FILE"
done
printf '%s\n' 'runtime argv-ok' >> "$SMOKE_ARGV_AUDIT_LOG"
exec "$SMOKE_REAL_RUNTIME" "$@"
`;

const fakeCurlScript = String.raw`#!/usr/bin/env bash
set -euo pipefail

snowflake_draft_id="1912345678901234567"

strict_failure() {
  printf '%s\n' "strict-failure:$1" >> "$SMOKE_FAKE_LOG"
  exit 64
}

for argument in "$@"; do
  if [[ "$argument" == *'"password"'* \
      || "$argument" == *'"title"'* \
      || "$argument" == *'Authorization: Bearer '* ]]; then
    printf '%s\n' 'curl argv-secret-detected' >> "$SMOKE_ARGV_AUDIT_LOG"
    exit 97
  fi
  while IFS= read -r secret || [[ -n "$secret" ]]; do
    [[ -n "$secret" ]] || continue
    if [[ "$argument" == *"$secret"* ]]; then
      printf '%s\n' 'curl argv-secret-detected' >> "$SMOKE_ARGV_AUDIT_LOG"
      exit 97
    fi
  done < "$SMOKE_SECRET_FILE"
done
printf '%s\n' 'curl argv-ok' >> "$SMOKE_ARGV_AUDIT_LOG"

method="GET"
output_file=""
request_body=""
authorization=""
write_status="false"
url=""
config_text=""
transport="argv"

while (( $# > 0 )); do
  case "$1" in
    -X|--request)
      [[ $# -ge 2 ]] || strict_failure "missing-request-value"
      method="$2"
      shift 2
      ;;
    -o|--output)
      [[ $# -ge 2 ]] || strict_failure "missing-output-value"
      output_file="$2"
      shift 2
      ;;
    -w|--write-out)
      [[ $# -ge 2 ]] || strict_failure "missing-write-out-value"
      [[ "$2" == "%{http_code}" ]] || strict_failure "unexpected-write-out"
      write_status="true"
      shift 2
      ;;
    -d|--data|--data-raw|--data-binary)
      [[ $# -ge 2 ]] || strict_failure "missing-data-value"
      request_body="$2"
      shift 2
      ;;
    -H|--header)
      [[ $# -ge 2 ]] || strict_failure "missing-header-value"
      if [[ "$2" == Authorization:* ]]; then
        authorization="$2"
      fi
      shift 2
      ;;
    --config)
      [[ $# -ge 2 ]] || strict_failure "missing-config-value"
      if [[ "$2" == "-" ]]; then
        config_text="$(cat)"
        transport="stdin-config"
      elif [[ -f "$2" ]]; then
        config_text="$(<"$2")"
        transport="file-config"
      else
        strict_failure "missing-config-file"
      fi
      shift 2
      ;;
    --url)
      [[ $# -ge 2 ]] || strict_failure "missing-url-value"
      [[ -z "$url" ]] || strict_failure "multiple-urls"
      url="$2"
      shift 2
      ;;
    --connect-timeout|--max-time)
      [[ $# -ge 2 ]] || strict_failure "missing-timeout-value"
      [[ "$2" =~ ^[0-9]+$ ]] || strict_failure "invalid-timeout"
      shift 2
      ;;
    -s|-S|-sS|--silent|--show-error)
      shift
      ;;
    http://*|https://*)
      [[ -z "$url" ]] || strict_failure "multiple-urls"
      url="$1"
      shift
      ;;
    *)
      strict_failure "unknown-option"
      ;;
  esac
done

if [[ -n "$config_text" ]]; then
  while IFS= read -r line; do
    [[ -n "$line" ]] || continue
    option="__DOLLAR__{line%%=*}"
    value="__DOLLAR__{line#*=}"
    option="__DOLLAR__{option#"__DOLLAR__{option%%[![:space:]]*}"}"
    option="__DOLLAR__{option%"__DOLLAR__{option##*[![:space:]]}"}"
    value="__DOLLAR__{value#"__DOLLAR__{value%%[![:space:]]*}"}"
    value="__DOLLAR__{value%"__DOLLAR__{value##*[![:space:]]}"}"
    if [[ "$value" == \"*\" ]]; then
      value="__DOLLAR__{value#\"}"
      value="__DOLLAR__{value%\"}"
    fi
    value="__DOLLAR__{value//\\\"/\"}"
    value="__DOLLAR__{value//\\\\/\\}"

    case "$option" in
      request)
        method="$value"
        ;;
      header)
        if [[ "$value" == Authorization:* ]]; then
          authorization="$value"
        fi
        ;;
      data-binary)
        [[ "$value" == @* ]] || strict_failure "inline-config-body"
        payload_file="__DOLLAR__{value#@}"
        [[ -f "$payload_file" ]] || strict_failure "missing-payload-file"
        request_body="$(<"$payload_file")"
        ;;
      *)
        strict_failure "unknown-config-option"
        ;;
    esac
  done <<< "$config_text"
fi

[[ "$method" =~ ^(GET|POST|DELETE)$ ]] || strict_failure "unknown-method"
[[ -n "$url" ]] || strict_failure "missing-url"
url_without_scheme="__DOLLAR__{url#*://}"
request_path="/__DOLLAR__{url_without_scheme#*/}"
route_path="__DOLLAR__{request_path%%\?*}"

admin_token="smoke-admin-token-secret"
teacher_token="smoke-teacher-token-secret"
student_token="smoke-student-token-secret"
actor="none"
case "$authorization" in
  "Authorization: Bearer $admin_token") actor="admin" ;;
  "Authorization: Bearer $teacher_token") actor="teacher" ;;
  "Authorization: Bearer $student_token") actor="student" ;;
  "") ;;
  *) actor="invalid" ;;
esac

body_state="none"
[[ -n "$request_body" ]] && body_state="file"
printf '%s\t%s\tactor=%s\tbody=%s\ttransport=%s\n' \
  "$method" "$request_path" "$actor" "$body_state" "$transport" \
  >> "$SMOKE_FAKE_LOG"

scenario="__DOLLAR__{SMOKE_FAKE_SCENARIO:-success}"
status="200"
body='{"code":200,"data":{}}'

if [[ "$method" == "POST" && "$route_path" == "/api/auth/login" ]]; then
  username="$(printf '%s' "$request_body" \
    | sed -n 's/.*"username":"\([^"]*\)".*/\1/p')"
  password="$(printf '%s' "$request_body" \
    | sed -n 's/.*"password":"\([^"]*\)".*/\1/p')"
  role=""
  token=""
  if [[ "$username" == "__DOLLAR__{SMOKE_ADMIN_USERNAME:-admin}" \
      && "$password" == "__DOLLAR__{SMOKE_ADMIN_PASSWORD:-admin123}" ]]; then
    role="admin"
    token="$admin_token"
  elif [[ "$username" == "__DOLLAR__{SMOKE_TEACHER_USERNAME:-t001}" \
      && "$password" == "__DOLLAR__{SMOKE_TEACHER_PASSWORD:-test123}" ]]; then
    role="teacher"
    token="$teacher_token"
  elif [[ "$username" == "__DOLLAR__{SMOKE_STUDENT_USERNAME:-teststu}" \
      && "$password" == "__DOLLAR__{SMOKE_STUDENT_PASSWORD:-test123}" ]]; then
    role="student"
    token="$student_token"
  fi

  if [[ "$scenario" == "token-failure" && "$role" == "admin" ]]; then
    status="401"
    body='{"code":401,"message":"登录失败"}'
  elif [[ "$scenario" == "token-crlf-injection" && "$role" == "admin" ]]; then
    body='{"code":200,"data":{"token":"safe-token\nurl = \"https://injected.invalid/\""}}'
  elif [[ "$scenario" == "token-invalid-character" && "$role" == "admin" ]]; then
    body='{"code":200,"data":{"token":"unsafe token"}}'
  elif [[ "$password" == "wrong" ]]; then
    [[ "$scenario" == "wrong-password-wrapper" ]] && status="200" || status="401"
    body='{"code":401,"message":"用户名或密码错误"}'
  elif [[ -n "$role" ]]; then
    body="{\"code\":200,\"data\":{\"token\":\"$token\"}}"
  else
    status="401"
    body='{"code":401,"message":"用户名或密码错误"}'
  fi
elif [[ "$method" == "GET" \
    && "$route_path" =~ ^/api/admin/(dashboard/stats|audit/list)$ ]]; then
  [[ "$actor" == "admin" ]] || strict_failure "admin-auth-contract"
elif [[ "$method" == "GET" && "$route_path" == "/api/v1/users" ]]; then
  [[ "$actor" == "admin" ]] || strict_failure "admin-auth-contract"
  body='{"items":[],"page":1,"size":20,"total":0}'
elif [[ "$method" == "GET" && "$route_path" == "/api/teacher/work/list" ]]; then
  if [[ "$actor" == "student" ]]; then
    [[ "$scenario" == "security-wrapper" ]] && status="200" || status="403"
    if [[ "$scenario" == "security-code-mismatch" ]]; then
      body='{"code":401,"message":"禁止访问"}'
    else
      body='{"code":403,"message":"禁止访问"}'
    fi
  else
    [[ "$actor" == "teacher" ]] || strict_failure "teacher-auth-contract"
  fi
elif [[ "$method" == "GET" && "$route_path" == "/api/teacher/ranking/list" ]]; then
  [[ "$actor" == "teacher" ]] || strict_failure "teacher-auth-contract"
elif [[ "$method" == "GET" && "$request_path" == "/api/student/works" ]]; then
  if [[ "$actor" == "none" ]]; then
    [[ "$scenario" == "security-wrapper" ]] && status="200" || status="401"
    body='{"code":401,"message":"未登录"}'
  else
    [[ "$actor" == "student" ]] || strict_failure "student-auth-contract"
    body='{"code":200,"data":{"records":[],"total":0,"pageNum":1,"pageSize":10,"pages":0}}'
  fi
elif [[ "$method" == "GET" \
    && "$request_path" =~ ^/api/student/works\?page=([1-3])\&size=50$ ]]; then
  [[ "$actor" == "student" ]] || strict_failure "student-auth-contract"
  page="__DOLLAR__{BASH_REMATCH[1]}"
  if [[ "$page" == "2" \
      && "$scenario" =~ ^create-(timeout|missing-id)$ \
      && -f "$SMOKE_FAKE_STATE_DIR/draft-title" ]]; then
    title="$(<"$SMOKE_FAKE_STATE_DIR/draft-title")"
    body="{\"code\":200,\"data\":{\"records\":[{\"id\":\"$snowflake_draft_id\",\"title\":\"$title\"}],\"total\":51,\"pageNum\":2,\"pageSize\":50,\"pages\":2}}"
  else
    body="{\"code\":200,\"data\":{\"records\":[],\"total\":0,\"pageNum\":$page,\"pageSize\":50,\"pages\":0}}"
  fi
elif [[ "$method" == "POST" && "$route_path" == "/api/student/works" ]]; then
  [[ "$actor" == "student" ]] || strict_failure "student-auth-contract"
  title="$(printf '%s' "$request_body" \
    | sed -n 's/.*"title":"\([^"]*\)".*/\1/p')"
  [[ -n "$title" ]] || strict_failure "missing-draft-title"
  printf '%s' "$title" > "$SMOKE_FAKE_STATE_DIR/draft-title"
  if [[ "$scenario" == "create-timeout" \
      || "$scenario" == "create-unrecoverable" ]]; then
    printf '%s\n' 'curl: (28) password=network-password-secret token=network-token-secret' >&2
    exit 28
  elif [[ "$scenario" == "create-missing-id" ]]; then
    body='{"code":200,"data":null}'
  else
    body="{\"code\":200,\"data\":\"$snowflake_draft_id\"}"
  fi
elif [[ "$method" == "DELETE" \
    && "$route_path" == "/api/student/works/$snowflake_draft_id" ]]; then
  [[ "$actor" == "student" ]] || strict_failure "student-auth-contract"
  if [[ "$scenario" == "cleanup-retry" ]]; then
    counter_file="$SMOKE_FAKE_STATE_DIR/delete-count"
    delete_count="0"
    [[ -f "$counter_file" ]] && delete_count="$(<"$counter_file")"
    delete_count="$((delete_count + 1))"
    printf '%s' "$delete_count" > "$counter_file"
    if [[ "$delete_count" == "1" ]]; then
      printf '%s\n' 'curl: (7) password=cleanup-password-secret token=cleanup-token-secret' >&2
      exit 7
    fi
  fi
  body='{"code":200,"data":null}'
elif [[ "$method" == "GET" && "$route_path" == "/api/public/works" ]]; then
  if [[ "$scenario" == "check-failure" ]]; then
    status="500"
    body='{"code":500,"message":"模拟检查失败"}'
  elif [[ "$scenario" == "sensitive-response-failure" ]]; then
    status="500"
    body='{"code":500,"message":"password=response-password-secret token=response-token-secret"}'
  elif [[ "$scenario" == "network-failure" ]]; then
    printf '%s\n' 'curl: (7) password=network-password-secret token=network-token-secret' >&2
    exit 7
  fi
elif [[ "$method" == "GET" && "$route_path" == "/api/public/ranking/list" ]]; then
  :
else
  strict_failure "unknown-route"
fi

if [[ -n "$output_file" ]]; then
  printf '%s' "$body" > "$output_file"
else
  printf '%s' "$body"
fi
[[ "$write_status" == "true" ]] && printf '%s' "$status"
`.replaceAll("__DOLLAR__", "$");

function resultText(result) {
  return `${result.stdout ?? ""}\n${result.stderr ?? ""}`;
}

async function runSmoke({
  mode,
  scenario = "success",
  credentials = {},
  runtime = "node",
} = {}) {
  const fixtureDirectory = await mkdtemp(
    path.join(tmpdir(), "jingxuan-smoke-contract-"),
  );
  const tempDirectory = path.join(fixtureDirectory, "tmp");
  const stateDirectory = path.join(fixtureDirectory, "state");
  const fakeCurlPath = path.join(fixtureDirectory, "fake-curl.sh");
  const runtimeGuardPath = path.join(fixtureDirectory, "runtime-guard.sh");
  const runnerPath = path.join(fixtureDirectory, "run-smoke.sh");
  const callLogPath = path.join(fixtureDirectory, "calls.log");
  const argvAuditPath = path.join(fixtureDirectory, "argv-audit.log");
  const secretFilePath = path.join(fixtureDirectory, "secrets.txt");
  let smokeResult;

  try {
    await mkdir(tempDirectory);
    await mkdir(stateDirectory);
    await writeFile(fakeCurlPath, fakeCurlScript, { mode: 0o700 });
    await writeFile(runtimeGuardPath, runtimeGuardScript, { mode: 0o700 });
    await chmod(fakeCurlPath, 0o700);
    await chmod(runtimeGuardPath, 0o700);

    const secretValues = [
      credentials.SMOKE_ADMIN_PASSWORD ?? "admin123",
      credentials.SMOKE_TEACHER_PASSWORD ?? "test123",
      credentials.SMOKE_STUDENT_PASSWORD ?? "test123",
      "smoke-admin-token-secret",
      "smoke-teacher-token-secret",
      "smoke-student-token-secret",
      "response-password-secret",
      "response-token-secret",
      "network-password-secret",
      "network-token-secret",
      "cleanup-password-secret",
      "cleanup-token-secret",
    ];
    await writeFile(
      secretFilePath,
      `${[...new Set(secretValues)].join("\n")}\n`,
      {
        mode: 0o600,
      },
    );

    const smokeEnvironment = {
      ...credentials,
      SMOKE_CURL_BIN: toBashPath(fakeCurlPath),
      SMOKE_FAKE_LOG: toBashPath(callLogPath),
      SMOKE_FAKE_SCENARIO: scenario,
      SMOKE_FAKE_STATE_DIR: toBashPath(stateDirectory),
      SMOKE_ARGV_AUDIT_LOG: toBashPath(argvAuditPath),
      SMOKE_SECRET_FILE: toBashPath(secretFilePath),
      TMPDIR: toBashPath(tempDirectory),
    };
    if (mode !== undefined) smokeEnvironment.SMOKE_MODE = mode;

    const unsetNames = [
      "SMOKE_MODE",
      "SMOKE_ADMIN_USERNAME",
      "SMOKE_ADMIN_PASSWORD",
      "SMOKE_TEACHER_USERNAME",
      "SMOKE_TEACHER_PASSWORD",
      "SMOKE_STUDENT_USERNAME",
      "SMOKE_STUDENT_PASSWORD",
      "SMOKE_NODE_BIN",
      "SMOKE_PYTHON_BIN",
    ];

    if (runtime === "python") {
      assert.ok(pythonExecutable, "当前环境没有 python3 运行时");
      smokeEnvironment.SMOKE_NODE_BIN = "jingxuan-missing-node-runtime";
      smokeEnvironment.SMOKE_PYTHON_BIN = toBashPath(runtimeGuardPath);
      smokeEnvironment.SMOKE_REAL_RUNTIME = pythonExecutable;
    } else {
      smokeEnvironment.SMOKE_NODE_BIN = toBashPath(runtimeGuardPath);
      smokeEnvironment.SMOKE_REAL_RUNTIME = nodeExecutable;
    }

    const exports = Object.entries(smokeEnvironment)
      .map(([name, value]) => `export ${name}=${shellQuote(value)}`)
      .join("\n");
    await writeFile(
      runnerPath,
      `#!/usr/bin/env bash\nset -euo pipefail\nunset ${unsetNames.join(" ")}\n${exports}\nexec bash scripts/smoke-test.sh https://smoke.invalid\n`,
      { mode: 0o700 },
    );

    const result = spawnSync("bash", [toBashPath(runnerPath)], {
      cwd: repositoryRoot,
      encoding: "utf8",
      env: process.env,
      timeout: 30_000,
    });

    const calls = await readFile(callLogPath, "utf8").catch(() => "");
    const argvAudit = await readFile(argvAuditPath, "utf8").catch(() => "");
    const tempFiles = await readdir(tempDirectory);
    smokeResult = {
      ...result,
      calls: calls.trim().split("\n").filter(Boolean),
      argvAudit,
      tempFiles,
      fixtureDirectory,
    };
  } finally {
    await rm(fixtureDirectory, { recursive: true, force: true });
  }

  return smokeResult;
}

async function runInvalidFakeCurl(method, requestPath) {
  const fixtureDirectory = await mkdtemp(
    path.join(tmpdir(), "jingxuan-smoke-fake-curl-"),
  );
  try {
    const fakeCurlPath = path.join(fixtureDirectory, "fake-curl.sh");
    const callLogPath = path.join(fixtureDirectory, "calls.log");
    const argvAuditPath = path.join(fixtureDirectory, "argv-audit.log");
    const secretFilePath = path.join(fixtureDirectory, "secrets.txt");
    const stateDirectory = path.join(fixtureDirectory, "state");
    const runnerPath = path.join(fixtureDirectory, "run-fake-curl.sh");
    await mkdir(stateDirectory);
    await writeFile(fakeCurlPath, fakeCurlScript, { mode: 0o700 });
    await writeFile(secretFilePath, "unused-secret\n", { mode: 0o600 });
    await writeFile(
      runnerPath,
      `#!/usr/bin/env bash\nset -euo pipefail\nexport SMOKE_FAKE_LOG=${shellQuote(toBashPath(callLogPath))}\nexport SMOKE_ARGV_AUDIT_LOG=${shellQuote(toBashPath(argvAuditPath))}\nexport SMOKE_SECRET_FILE=${shellQuote(toBashPath(secretFilePath))}\nexport SMOKE_FAKE_STATE_DIR=${shellQuote(toBashPath(stateDirectory))}\nexec ${shellQuote(toBashPath(fakeCurlPath))} -sS -o ${shellQuote(toBashPath(path.join(fixtureDirectory, "response.json")))} -w '%{http_code}' -X ${shellQuote(method)} ${shellQuote(`https://smoke.invalid${requestPath}`)}\n`,
      { mode: 0o700 },
    );
    return spawnSync("bash", [toBashPath(runnerPath)], {
      encoding: "utf8",
      env: process.env,
    });
  } finally {
    await rm(fixtureDirectory, { recursive: true, force: true });
  }
}

test("默认模式只执行只读检查且 fake curl 日志已脱敏", async () => {
  const result = await runSmoke();

  assert.equal(result.status, 0, resultText(result));
  assert.ok(result.calls.some((line) => line.includes("actor=admin")));
  assert.ok(result.calls.some((line) => line.includes("actor=teacher")));
  assert.ok(result.calls.some((line) => line.includes("actor=student")));
  assert.equal(
    result.calls.some((line) => line.startsWith("POST\t/api/student/works\t")),
    false,
  );
  assert.equal(
    result.calls.some((line) => line.startsWith("DELETE\t")),
    false,
  );
  assert.doesNotMatch(result.calls.join("\n"), /password|Bearer|token-secret/u);
});

test("三角色凭据可通过环境变量覆盖且不进入日志", async () => {
  const credentials = {
    SMOKE_ADMIN_USERNAME: "ops-admin",
    SMOKE_ADMIN_PASSWORD: "admin-secret",
    SMOKE_TEACHER_USERNAME: "teacher-x",
    SMOKE_TEACHER_PASSWORD: "teacher-secret",
    SMOKE_STUDENT_USERNAME: "student-x",
    SMOKE_STUDENT_PASSWORD: "student-secret",
  };
  const result = await runSmoke({ credentials });

  assert.equal(result.status, 0, result.stderr || result.stdout);
  for (const secret of Object.values(credentials).filter((value) =>
    value.endsWith("secret"),
  )) {
    assert.doesNotMatch(
      `${result.calls.join("\n")}\n${resultText(result)}`,
      new RegExp(secret, "u"),
    );
  }
});

test("任一普通检查失败时继续汇总并最终返回非零状态", async () => {
  const result = await runSmoke({ scenario: "check-failure" });

  assert.equal(result.status, 1, result.stderr || result.stdout);
  assert.match(result.stdout, /公开排行榜/u);
  assert.match(result.stdout, /存在失败项/u);
});

test("token 获取失败时立即退出且不再请求其他接口", async () => {
  const result = await runSmoke({ scenario: "token-failure" });

  assert.equal(result.status, 1, result.stderr || result.stdout);
  assert.equal(result.calls.length, 1, result.calls.join("\n"));
  assert.match(resultText(result), /管理员.*token/u);
});

test("拒绝带 CRLF 的 Bearer token，且不会把它解释成 curl 配置", async () => {
  const result = await runSmoke({ scenario: "token-crlf-injection" });

  assert.equal(result.status, 1, resultText(result));
  assert.equal(result.calls.length, 1, result.calls.join("\n"));
  assert.doesNotMatch(resultText(result), /injected\.invalid/u);
});

test("拒绝含空格等非法字符的 Bearer token", async () => {
  const result = await runSmoke({ scenario: "token-invalid-character" });

  assert.equal(result.status, 1, resultText(result));
  assert.equal(result.calls.length, 1, result.calls.join("\n"));
});

test("curl URL 使用显式 --url 参数，避免把外部地址解释成选项", async () => {
  const source = await readRepositoryFile("scripts/smoke-test.sh");

  assert.match(source, /--url\s+"\$BASE_URL\$path"/u);
});

test("临时目录始终由 EXIT trap 清理，fixture 位于系统临时目录", async () => {
  const source = await readFile(smokeScript, "utf8");
  const result = await runSmoke({ scenario: "token-failure" });

  assert.match(source, /trap\s+['"]?cleanup_on_exit/u);
  assert.deepEqual(result.tempFiles, []);
  assert.ok(result.fixtureDirectory.startsWith(tmpdir()));
  await assert.rejects(access(result.fixtureDirectory), { code: "ENOENT" });
});

test("只有 full 模式创建草稿，并以雪花字符串 ID 回收", async () => {
  const result = await runSmoke({ mode: "full" });

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.equal(
    result.calls.filter((line) => line.startsWith("POST\t/api/student/works\t"))
      .length,
    1,
  );
  assert.equal(
    result.calls.filter((line) =>
      line.startsWith(`DELETE\t/api/student/works/${snowflakeDraftId}\t`),
    ).length,
    1,
  );
});

test("正常清理失败时 EXIT trap 会重试回收草稿且保留失败状态", async () => {
  const result = await runSmoke({ mode: "full", scenario: "cleanup-retry" });

  assert.equal(result.status, 1, result.stderr || result.stdout);
  assert.equal(
    result.calls.filter((line) =>
      line.startsWith(`DELETE\t/api/student/works/${snowflakeDraftId}\t`),
    ).length,
    2,
  );
  assert.deepEqual(result.tempFiles, []);
});

test("full 模式在普通检查失败后仍回收已创建草稿", async () => {
  const result = await runSmoke({ mode: "full", scenario: "check-failure" });

  assert.equal(result.status, 1, result.stderr || result.stdout);
  assert.equal(
    result.calls.filter((line) =>
      line.startsWith(`DELETE\t/api/student/works/${snowflakeDraftId}\t`),
    ).length,
    1,
  );
});

test("安全检查不接受 HTTP 200 包装的 401/403 业务码", async () => {
  const result = await runSmoke({ scenario: "security-wrapper" });

  assert.equal(result.status, 1, result.stderr || result.stdout);
  assert.match(result.stdout, /越权拦截.*HTTP=200.*code=403/u);
  assert.match(result.stdout, /匿名访问拦截.*HTTP=200.*code=401/u);
});

test("安全检查同时校验真实 HTTP 状态与业务码", async () => {
  const result = await runSmoke({ scenario: "security-code-mismatch" });

  assert.equal(result.status, 1, result.stderr || result.stdout);
  assert.match(result.stdout, /越权拦截.*HTTP=403.*code=401/u);
});

test("错误密码检查单独兼容 legacy HTTP 200 包装", async () => {
  const result = await runSmoke({ scenario: "wrong-password-wrapper" });

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.match(result.stdout, /错误密码拒绝/u);
});

test("失败输出不回显含 password/token 的原始响应", async () => {
  const result = await runSmoke({ scenario: "sensitive-response-failure" });

  assert.equal(result.status, 1, result.stderr || result.stdout);
  assert.match(result.stdout, /HTTP=500.*code=500/u);
  assert.doesNotMatch(
    resultText(result),
    /response-(?:password|token)-secret/u,
  );
});

test("网络失败只输出无敏感的 curl 退出码", async () => {
  const result = await runSmoke({ scenario: "network-failure" });

  assert.equal(result.status, 1, result.stderr || result.stdout);
  assert.match(result.stdout, /HTTP=000.*code=unknown.*network=curl-exit-7/u);
  assert.doesNotMatch(resultText(result), /network-(?:password|token)-secret/u);
});

test("创建超时后通过预先生成的唯一标题有界查询并回收", async () => {
  const result = await runSmoke({ mode: "full", scenario: "create-timeout" });

  assert.equal(result.status, 1, result.stderr || result.stdout);
  const recoveryCalls = result.calls.filter((line) =>
    line.startsWith("GET\t/api/student/works?page="),
  );
  assert.ok(recoveryCalls.length > 0 && recoveryCalls.length <= 3);
  assert.ok(recoveryCalls.some((line) => line.includes("page=2&size=50")));
  assert.equal(
    result.calls.filter((line) =>
      line.startsWith(`DELETE\t/api/student/works/${snowflakeDraftId}\t`),
    ).length,
    1,
  );
});

test("创建成功但响应缺 ID 时仍按唯一标题定位并回收", async () => {
  const result = await runSmoke({
    mode: "full",
    scenario: "create-missing-id",
  });

  assert.equal(result.status, 1, result.stderr || result.stdout);
  assert.ok(
    result.calls.some((line) =>
      line.startsWith(`DELETE\t/api/student/works/${snowflakeDraftId}\t`),
    ),
  );
});

test("创建结果无法定位时非零退出且只打印自生成标题标记", async () => {
  const result = await runSmoke({
    mode: "full",
    scenario: "create-unrecoverable",
  });

  assert.equal(result.status, 1, result.stderr || result.stdout);
  assert.match(resultText(result), /title-marker=smoke-draft-[0-9-]+/u);
  assert.equal(
    result.calls.some((line) => line.startsWith("DELETE\t")),
    false,
  );
  assert.doesNotMatch(
    resultText(result),
    /admin123|test123|smoke-(?:admin|teacher|student)-token-secret/u,
  );
});

test("password、JSON body 和 Bearer token 都不进入 Node/curl argv", async () => {
  const result = await runSmoke({
    mode: "full",
    credentials: {
      SMOKE_ADMIN_PASSWORD: "argv-admin-password-secret",
      SMOKE_TEACHER_PASSWORD: "argv-teacher-password-secret",
      SMOKE_STUDENT_PASSWORD: "argv-student-password-secret",
    },
  });

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.match(result.argvAudit, /runtime argv-ok/u);
  assert.match(result.argvAudit, /curl argv-ok/u);
  assert.doesNotMatch(result.argvAudit, /argv-secret-detected/u);
  assert.ok(
    result.calls.every((line) => line.includes("transport=stdin-config")),
  );
});

test(
  "password、JSON body 和 Bearer token 都不进入 Python/curl argv",
  { skip: pythonExecutable === null },
  async () => {
    const result = await runSmoke({ mode: "full", runtime: "python" });

    assert.equal(result.status, 0, result.stderr || result.stdout);
    assert.doesNotMatch(result.argvAudit, /argv-secret-detected/u);
    assert.ok(
      result.calls.every((line) => line.includes("transport=stdin-config")),
    );
  },
);

test("fake curl 对未知方法与未知路径都严格失败", async () => {
  const unknownMethod = await runInvalidFakeCurl("PATCH", "/api/public/works");
  const unknownPath = await runInvalidFakeCurl("GET", "/unknown/path");

  assert.notEqual(unknownMethod.status, 0);
  assert.notEqual(unknownPath.status, 0);
});

test("Flyway V1 引用的 14 个 legacy SQL 全部随后端主资源打包", async () => {
  const migrationSource = await readRepositoryFile(
    "backend/src/main/java/db/migration/V1__Baseline.java",
  );
  const pom = await readRepositoryFile("backend/pom.xml");
  const referencedScripts = [
    ...migrationSource.matchAll(/"(legacy-sql\/[^"]+\.sql)"/gu),
  ].map((match) => match[1]);

  assert.equal(referencedScripts.length, 14);
  for (const script of referencedScripts) {
    const resourcePath = path.join(
      repositoryRoot,
      "backend",
      "src",
      "main",
      "resources",
      ...script.split("/"),
    );
    await assert.doesNotReject(
      readFile(resourcePath, "utf8"),
      `${script} 必须位于 backend/src/main/resources 中`,
    );
  }

  assert.doesNotMatch(pom, /\.\.\/sql|\.\.\\sql/u);
  assert.equal(
    referencedScripts.some((script) => /test[-_]data/u.test(script)),
    false,
    "测试 fixture 不得进入主资源",
  );
  await assert.rejects(readdir(path.join(repositoryRoot, "sql")), {
    code: "ENOENT",
  });
  await assert.rejects(readdir(path.join(repositoryRoot, "backend", "sql")), {
    code: "ENOENT",
  });
});

test("生产 JSON 日志只使用 Spring Boot 内置结构化编码器", async () => {
  const logback = await readRepositoryFile(
    "backend/src/main/resources/logback-spring.xml",
  );

  assert.match(
    logback,
    /org\.springframework\.boot\.logging\.logback\.StructuredLogEncoder/u,
  );
  assert.match(logback, /<format>logstash<\/format>/u);
  assert.doesNotMatch(logback, /ch\.qos\.logback\.contrib/u);
});

test("Compose 固定安全 project、镜像版本并兼容旧 MySQL 卷迁移", async () => {
  const compose = await readRepositoryFile("docker-compose.yml");

  assert.match(compose, /^name:\s*jingxuan$/mu);
  assert.equal(compose.match(/image:\s*mysql:8\.0\.46/gu)?.length, 2);
  assert.match(compose, /image:\s*redis:7\.4\.5-alpine/u);
  assert.doesNotMatch(compose, /container_name\s*:/u);
  assert.doesNotMatch(compose, /\$\{DB_PASSWORD:-/u);
  assert.match(
    compose,
    /MYSQL_ROOT_PASSWORD:\s*\$\{DB_ROOT_PASSWORD:-\$\{DB_PASSWORD:\?DB_PASSWORD is required\}\}/u,
  );
  assert.match(compose, /MYSQL_USER:\s*\$\{DB_USER:-jingxuan\}/u);
  assert.match(compose, /MYSQL_PASSWORD:\s*\$\{DB_PASSWORD:\?/u);
  assert.match(compose, /mysql-user-bootstrap:/u);
  const mysqlHealthcheck = compose.match(
    /mysql:\s*[\s\S]*?healthcheck:\s*([\s\S]*?)\n\s{4}networks:/u,
  )?.[1];
  assert.ok(mysqlHealthcheck, "MySQL 必须配置健康检查");
  assert.match(mysqlHealthcheck, /mysqladmin\s+ping/u);
  assert.doesNotMatch(mysqlHealthcheck, /MYSQL_(?:USER|PASSWORD|PWD)|-u\s/u);
  assert.match(
    compose,
    /LEGACY_ROOT_PASSWORD:\s*\$\{DB_LEGACY_ROOT_PASSWORD:-\$\{DB_PASSWORD:\?DB_PASSWORD is required\}\}/u,
  );
  assert.match(compose, /condition:\s*service_completed_successfully/u);
  assert.match(compose, /DB_USER:\s*\$\{DB_USER:-jingxuan\}/u);
  assert.match(compose, /DB_PASSWORD:\s*\$\{DB_PASSWORD:\?/u);
  assert.doesNotMatch(compose, /^\s+DB_USER:\s*root\s*$/mu);
  assert.match(compose, /JWT_SECRET:\s*\$\{JWT_SECRET:\?/u);
  assert.doesNotMatch(compose, /(?:\.\/)?sql\//u);
});

test("CI 的 legacy 运行时冒烟有界等待并验证 Flyway 与 fixture 顺序", async () => {
  const workflow = await readRepositoryFile(".github/workflows/ci.yml");
  const job = extractWorkflowJob(workflow, "legacy-runtime-smoke");

  assert.match(
    job,
    /COMPOSE_PROJECT_NAME:[^\n]*github\.run_id[^\n]*github\.run_attempt/u,
  );
  assert.match(job, /docker compose up[^\n]*mysql redis backend/u);
  assert.match(job, /初始化旧版 MySQL 卷/u);
  assert.match(job, /mysql:8\.0\.46/u);
  assert.match(job, /DB_LEGACY_ROOT_PASSWORD/u);
  assert.match(job, /legacy_ci_sentinel/u);
  assert.match(job, /旧 root 密码仍可登录|旧 root 密码必须失效/u);
  assert.match(job, /应用账号无法读取旧卷数据|preserved-before-migration/u);
  assert.match(job, /deadline=\$\(\(SECONDS \+ \d+\)\)/u);
  assert.match(job, /State\.Status/u);
  assert.match(job, /State\.Restarting/u);
  assert.match(job, /flyway_schema_history/u);
  assert.match(job, /version[^\n]*["']1["'][^\n]*["']2["']/u);
  assert.match(job, /sys_user/u);
  assert.match(job, /EVENT_PUBLICATION/u);

  const flushIndex = job.indexOf("FLUSHDB");
  const transactionIndex = job.indexOf("START TRANSACTION");
  const cleanupIndex = job.indexOf(
    "backend/src/test/resources/sql/cleanup.sql",
  );
  const testDataIndex = job.indexOf(
    "backend/src/test/resources/sql/test-data.sql",
  );
  const commitIndex = job.indexOf("COMMIT");
  assert.ok(flushIndex !== -1, "装载 fixture 前必须清空 Redis");
  assert.ok(transactionIndex > flushIndex, "fixture 必须在 FLUSHDB 后开始事务");
  assert.ok(cleanupIndex > transactionIndex, "事务内必须先执行 cleanup.sql");
  assert.ok(testDataIndex > cleanupIndex, "cleanup.sql 必须先于 test-data.sql");
  assert.ok(commitIndex > testDataIndex, "fixture 必须在同一事务提交");
  assert.match(job, /SMOKE_MODE:\s*full/u);
  assert.match(job, /npm run smoke:legacy/u);
  assert.doesNotMatch(job, /(?:^|[\s"'])\.?\/?sql\//mu);
  assert.doesNotMatch(job, /(?:^|[\s"'])\.env(?:[\s"']|$)/mu);
  assert.doesNotMatch(job, /mysqldump|docker compose exec[^\n]*dump/iu);
});

test("CI 失败时只收集脱敏日志且始终销毁卷与孤儿容器", async () => {
  const workflow = await readRepositoryFile(".github/workflows/ci.yml");
  const job = extractWorkflowJob(workflow, "legacy-runtime-smoke");

  assert.match(job, /if:\s*failure\(\)/u);
  assert.match(job, /docker compose ps/u);
  assert.match(job, /docker compose logs[^\n]*(?:mysql redis backend|mysql)/u);
  assert.match(job, /DB_PASSWORD[^\n]*已脱敏|已脱敏[^\n]*DB_PASSWORD/u);
  assert.match(
    job,
    /DB_LEGACY_ROOT_PASSWORD[^\n]*已脱敏|已脱敏[^\n]*DB_LEGACY_ROOT_PASSWORD/u,
  );
  assert.match(job, /JWT_SECRET[^\n]*已脱敏|已脱敏[^\n]*JWT_SECRET/u);
  assert.equal(job.match(/::add-mask::/gu)?.length, 1);
  assert.match(job, /Object\.values\(values\)/u);
  assert.doesNotMatch(job, /docker inspect[^\n]*(?:Config\.Env|\.Config)/u);
  assert.match(job, /if:\s*always\(\)/u);
  assert.match(
    job,
    /docker compose down[^\n]*--volumes[^\n]*--remove-orphans/u,
  );
});

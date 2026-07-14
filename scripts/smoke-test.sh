#!/usr/bin/env bash
# 菁选校园作品展示平台 — 冒烟测试脚本
# 用法: bash scripts/smoke-test.sh [http://localhost:8080]
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
SMOKE_MODE="${SMOKE_MODE:-read-only}"
CURL_BIN="${SMOKE_CURL_BIN:-curl}"
NODE_BIN="${SMOKE_NODE_BIN:-node}"
PYTHON_BIN="${SMOKE_PYTHON_BIN:-python3}"

ADMIN_USERNAME="${SMOKE_ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${SMOKE_ADMIN_PASSWORD:-admin123}"
TEACHER_USERNAME="${SMOKE_TEACHER_USERNAME:-t001}"
TEACHER_PASSWORD="${SMOKE_TEACHER_PASSWORD:-test123}"
STUDENT_USERNAME="${SMOKE_STUDENT_USERNAME:-teststu}"
STUDENT_PASSWORD="${SMOKE_STUDENT_PASSWORD:-test123}"

PASS=0
FAIL=0
ADMIN_TOKEN=""
TEACHER_TOKEN=""
STUDENT_TOKEN=""
DRAFT_ID=""
DRAFT_TITLE=""

command_available() {
  if [[ "$1" == */* ]]; then
    [[ -x "$1" ]]
  else
    command -v "$1" >/dev/null 2>&1
  fi
}

if ! command_available "$CURL_BIN"; then
  printf '❌ curl 不可用：%s\n' "$CURL_BIN" >&2
  exit 1
fi

if command_available "$NODE_BIN"; then
  JSON_RUNTIME="node"
  JSON_BIN="$NODE_BIN"
elif command_available "$PYTHON_BIN"; then
  JSON_RUNTIME="python"
  JSON_BIN="$PYTHON_BIN"
else
  printf '❌ 需要 Node.js 或 Python 3 解析 JSON 响应\n' >&2
  exit 1
fi

case "$SMOKE_MODE" in
  read-only|full) ;;
  *)
    printf '❌ SMOKE_MODE 仅支持 read-only 或 full\n' >&2
    exit 1
    ;;
esac

WORK_DIR=$(mktemp -d "${TMPDIR:-/tmp}/jingxuan-smoke.XXXXXX")
RESPONSE_FILE="$WORK_DIR/response.json"
CURL_ERROR_FILE="$WORK_DIR/curl-error.log"
PAYLOAD_FILE="$WORK_DIR/request.json"

HTTP_STATUS="000"
BODY_CODE="unknown"
NETWORK_STATUS=""

run_json() {
  local node_program="$1"
  local python_program="$2"
  if [[ "$JSON_RUNTIME" == "node" ]]; then
    "$JSON_BIN" -e "$node_program"
  else
    "$JSON_BIN" -c "$python_program"
  fi
}

json_code() {
  run_json \
    'const fs=require("fs");try{const value=JSON.parse(fs.readFileSync(0,"utf8")).code;if(value!==undefined&&value!==null)process.stdout.write(String(value));}catch{}' \
    'import json,sys
try:
 value=json.load(sys.stdin).get("code")
 sys.stdout.write("" if value is None else str(value))
except Exception:
 pass'
}

json_token() {
  run_json \
    'const fs=require("fs");try{const value=JSON.parse(fs.readFileSync(0,"utf8"))?.data?.token;if(value!==undefined&&value!==null)process.stdout.write(String(value));}catch{}' \
    'import json,sys
try:
 data=json.load(sys.stdin).get("data") or {}
 value=data.get("token") if isinstance(data,dict) else None
 sys.stdout.write("" if value is None else str(value))
except Exception:
 pass'
}

valid_bearer_token() {
  local token="$1"
  [[ "$token" =~ ^[A-Za-z0-9._~-]+$ ]]
}

json_draft_id() {
  run_json \
    'const fs=require("fs");try{const data=JSON.parse(fs.readFileSync(0,"utf8"))?.data;const value=data&&typeof data==="object"?data.id:data;if(value!==undefined&&value!==null)process.stdout.write(String(value));}catch{}' \
    'import json,sys
try:
 data=json.load(sys.stdin).get("data")
 value=data.get("id") if isinstance(data,dict) else data
 sys.stdout.write("" if value is None else str(value))
except Exception:
 pass'
}

json_matching_draft_id() {
  run_json \
    'const fs=require("fs");try{const records=JSON.parse(fs.readFileSync(0,"utf8"))?.data?.records??[];const match=records.find((item)=>item?.title===process.env.SMOKE_EXPECTED_DRAFT_TITLE);if(match?.id!==undefined&&match?.id!==null)process.stdout.write(String(match.id));}catch{}' \
    'import json,os,sys
try:
 records=((json.load(sys.stdin).get("data") or {}).get("records") or [])
 match=next((item for item in records if item.get("title")==os.environ.get("SMOKE_EXPECTED_DRAFT_TITLE")),None)
 if match is not None and match.get("id") is not None:
  sys.stdout.write(str(match.get("id")))
except Exception:
 pass'
}

write_login_payload() {
  local username="$1"
  local password="$2"
  SMOKE_JSON_USERNAME="$username" SMOKE_JSON_PASSWORD="$password" run_json \
    'process.stdout.write(JSON.stringify({username:process.env.SMOKE_JSON_USERNAME,password:process.env.SMOKE_JSON_PASSWORD}));' \
    'import json,os,sys
json.dump({"username":os.environ.get("SMOKE_JSON_USERNAME",""),"password":os.environ.get("SMOKE_JSON_PASSWORD","")},sys.stdout,separators=(",",":"))'
}

write_draft_payload() {
  SMOKE_JSON_TITLE="$1" run_json \
    'process.stdout.write(JSON.stringify({title:process.env.SMOKE_JSON_TITLE,summary:"自动化冒烟测试草稿",techStack:"Java"}));' \
    'import json,os,sys
json.dump({"title":os.environ.get("SMOKE_JSON_TITLE",""),"summary":"自动化冒烟测试草稿","techStack":"Java"},sys.stdout,separators=(",",":"))'
}

curl_config_quote() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  printf '"%s"' "$value"
}

perform_request() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local has_payload="${4:-false}"
  local config
  local curl_exit

  : > "$RESPONSE_FILE"
  : > "$CURL_ERROR_FILE"

  config="request = $(curl_config_quote "$method")"
  if [[ -n "$token" ]]; then
    config+=$'\n'"header = $(curl_config_quote "Authorization: Bearer $token")"
  fi
  if [[ "$has_payload" == "true" ]]; then
    config+=$'\n'"header = $(curl_config_quote "Content-Type: application/json")"
    config+=$'\n'"data-binary = $(curl_config_quote "@$PAYLOAD_FILE")"
  fi
  config+=$'\n'

  set +e
  HTTP_STATUS=$(printf '%s' "$config" | "$CURL_BIN" \
    -sS \
    -o "$RESPONSE_FILE" \
    -w '%{http_code}' \
    --connect-timeout 5 \
    --max-time 15 \
    --config - \
    --url "$BASE_URL$path" \
    2> "$CURL_ERROR_FILE")
  curl_exit=$?
  set -e

  if [[ "$curl_exit" -ne 0 ]]; then
    HTTP_STATUS="000"
    NETWORK_STATUS="curl-exit-$curl_exit"
  else
    NETWORK_STATUS=""
  fi

  BODY_CODE=$(json_code < "$RESPONSE_FILE" 2>/dev/null || true)
  [[ -n "$BODY_CODE" ]] || BODY_CODE="unknown"
}

failure_details() {
  printf 'HTTP=%s code=%s' "$HTTP_STATUS" "$BODY_CODE"
  if [[ -n "$NETWORK_STATUS" ]]; then
    printf ' network=%s' "$NETWORK_STATUS"
  fi
}

record_success() {
  printf '  ✅ %s\n' "$1"
  PASS=$((PASS + 1))
}

record_failure() {
  printf '  ❌ %s (%s)\n' "$1" "$(failure_details)"
  FAIL=$((FAIL + 1))
}

check_success() {
  local description="$1"
  local method="$2"
  local path="$3"
  local token="${4:-}"
  perform_request "$method" "$path" "$token"
  if [[ "$HTTP_STATUS" == "200" && "$BODY_CODE" == "200" ]]; then
    record_success "$description"
  else
    record_failure "$description"
  fi
}

check_http_success() {
  local description="$1"
  local method="$2"
  local path="$3"
  local token="${4:-}"
  perform_request "$method" "$path" "$token"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    record_success "$description"
  else
    record_failure "$description"
  fi
}

check_expected_error() {
  local description="$1"
  local method="$2"
  local path="$3"
  local expected_status="$4"
  local expected_code="$5"
  local token="${6:-}"
  local allow_legacy_wrapper="${7:-false}"
  local has_payload="${8:-false}"
  perform_request "$method" "$path" "$token" "$has_payload"

  if [[ "$BODY_CODE" == "$expected_code" \
      && ( "$HTTP_STATUS" == "$expected_status" \
        || ( "$allow_legacy_wrapper" == "true" && "$HTTP_STATUS" == "200" ) ) ]]; then
    record_success "$description"
  else
    record_failure "$description"
  fi
}

login() {
  local role_name="$1"
  local username="$2"
  local password="$3"
  local token

  write_login_payload "$username" "$password" > "$PAYLOAD_FILE"
  perform_request "POST" "/api/auth/login" "" "true"
  token=$(json_token < "$RESPONSE_FILE" 2>/dev/null || true)
  if [[ "$HTTP_STATUS" != "200" \
      || "$BODY_CODE" != "200" \
      || -z "$token" ]] \
      || ! valid_bearer_token "$token"; then
    printf '  ❌ %s token 获取失败 (%s)\n' "$role_name" "$(failure_details)" >&2
    return 1
  fi

  printf '%s' "$token"
}

valid_draft_id() {
  [[ "$1" =~ ^[0-9]{16,20}$ ]]
}

recover_draft_id() {
  local page
  local recovered_id
  for page in 1 2 3; do
    perform_request \
      "GET" \
      "/api/student/works?page=$page&size=50" \
      "$STUDENT_TOKEN"
    if [[ "$HTTP_STATUS" != "200" || "$BODY_CODE" != "200" ]]; then
      continue
    fi
    recovered_id=$(
      SMOKE_EXPECTED_DRAFT_TITLE="$DRAFT_TITLE" \
        json_matching_draft_id < "$RESPONSE_FILE" 2>/dev/null || true
    )
    if valid_draft_id "$recovered_id"; then
      DRAFT_ID="$recovered_id"
      return 0
    fi
  done
  return 1
}

delete_draft() {
  [[ -n "$DRAFT_ID" ]] || return 0
  perform_request \
    "DELETE" \
    "/api/student/works/$DRAFT_ID" \
    "$STUDENT_TOKEN"
  if [[ "$HTTP_STATUS" == "200" && "$BODY_CODE" == "200" ]]; then
    DRAFT_ID=""
    return 0
  fi
  return 1
}

cleanup_on_exit() {
  local status=$?
  trap - EXIT
  set +e

  if [[ -n "${DRAFT_ID:-}" && -n "${STUDENT_TOKEN:-}" ]]; then
    if ! delete_draft; then
      status=1
    fi
  fi

  rm -rf -- "${WORK_DIR:-}"
  exit "$status"
}
trap 'cleanup_on_exit' EXIT

printf '\n========================================\n'
printf '  菁选 — 冒烟测试（%s）\n' "$SMOKE_MODE"
printf '========================================\n\n'

ADMIN_TOKEN=$(login "管理员" "$ADMIN_USERNAME" "$ADMIN_PASSWORD") || exit 1
TEACHER_TOKEN=$(login "教师" "$TEACHER_USERNAME" "$TEACHER_PASSWORD") || exit 1
STUDENT_TOKEN=$(login "学生" "$STUDENT_USERNAME" "$STUDENT_PASSWORD") || exit 1

printf '【1/6】认证\n'
record_success "三角色登录"
write_login_payload "$ADMIN_USERNAME" "wrong" > "$PAYLOAD_FILE"
check_expected_error \
  "错误密码拒绝" \
  "POST" \
  "/api/auth/login" \
  "401" \
  "401" \
  "" \
  "true" \
  "true"

printf '【2/6】管理端\n'
check_success "仪表盘统计" "GET" "/api/admin/dashboard/stats" "$ADMIN_TOKEN"
check_success "审核列表" "GET" "/api/admin/audit/list" "$ADMIN_TOKEN"
check_http_success "用户列表" "GET" "/api/v1/users" "$ADMIN_TOKEN"

printf '【3/6】教师端\n'
check_success "待评分作品列表" "GET" "/api/teacher/work/list" "$TEACHER_TOKEN"
check_success "教师排行榜" "GET" "/api/teacher/ranking/list" "$TEACHER_TOKEN"

printf '【4/6】学生端\n'
check_success "我的作品列表" "GET" "/api/student/works" "$STUDENT_TOKEN"

if [[ "$SMOKE_MODE" == "full" ]]; then
  DRAFT_TITLE="smoke-draft-$(date +%s)-$$-$RANDOM"
  write_draft_payload "$DRAFT_TITLE" > "$PAYLOAD_FILE"
  perform_request "POST" "/api/student/works" "$STUDENT_TOKEN" "true"
  candidate_id=$(json_draft_id < "$RESPONSE_FILE" 2>/dev/null || true)

  if [[ "$HTTP_STATUS" == "200" \
      && "$BODY_CODE" == "200" \
      && -n "$candidate_id" ]] \
      && valid_draft_id "$candidate_id"; then
    DRAFT_ID="$candidate_id"
    record_success "创建临时草稿"
  else
    record_failure "创建临时草稿"
    if ! recover_draft_id; then
      printf '  ℹ️  无法定位可能已创建的草稿，title-marker=%s\n' "$DRAFT_TITLE"
    fi
  fi
fi

printf '【5/6】前台\n'
check_success "公开作品列表" "GET" "/api/public/works"
check_success "公开排行榜" "GET" "/api/public/ranking/list"

printf '【6/6】安全\n'
check_expected_error \
  "越权拦截（学生→教师）" \
  "GET" \
  "/api/teacher/work/list" \
  "403" \
  "403" \
  "$STUDENT_TOKEN"
check_expected_error \
  "匿名访问拦截" \
  "GET" \
  "/api/student/works" \
  "401" \
  "401"

if [[ -n "$DRAFT_ID" ]]; then
  if delete_draft; then
    record_success "回收临时草稿"
  else
    record_failure "回收临时草稿"
  fi
fi

printf '\n========================================\n'
printf '  结果: %s 通过 / %s 失败\n' "$PASS" "$FAIL"
printf '========================================\n'
if [[ "$FAIL" -eq 0 ]]; then
  printf '  ✅ 冒烟测试通过\n\n'
  exit 0
fi

printf '  ❌ 存在失败项\n\n'
exit 1

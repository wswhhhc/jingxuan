#!/usr/bin/env bash
# 菁选校园作品展示平台 — 冒烟测试脚本
# 用法: bash smoke-test.sh [http://localhost:8080]
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
PASS=0
FAIL=0

check() {
    local desc="$1" method="$2" url="$3" expect="$4"
    local token="${5:-}" data="${6:-}"
    local extra=()
    [ -n "$token" ] && extra=(-H "Authorization: Bearer $token")
    local resp
    if [ "$method" = "POST" ]; then
        resp=$(curl -s -o /tmp/smoke-resp.txt -w "%{http_code}" -X POST \
            "${extra[@]}" -H "Content-Type: application/json" \
            -d "$data" "$BASE_URL$url" 2>/dev/null || echo "000")
    else
        resp=$(curl -s -o /tmp/smoke-resp.txt -w "%{http_code}" \
            "${extra[@]}" "$BASE_URL$url" 2>/dev/null || echo "000")
    fi
    # 对 POST 登录使用 JSON body 中的 code 字段判断
    if [ "$method" = "POST" ] && echo "$url" | grep -q "/login"; then
        local code
        code=$(python3 -c "import sys,json; print(json.load(open('/tmp/smoke-resp.txt'))['code'])" 2>/dev/null || echo "$resp")
        if echo "$code" | grep -qE "$expect"; then
            echo "  ✅ $desc"
            PASS=$((PASS + 1))
        else
            echo "  ❌ $desc (期望 $expect, 实际 code=$code)"
            FAIL=$((FAIL + 1))
        fi
    elif echo "$resp" | grep -qE "$expect"; then
        echo "  ✅ $desc"
        PASS=$((PASS + 1))
    else
        echo "  ❌ $desc (期望 $expect, 实际 $resp)"
        cat /tmp/smoke-resp.txt 2>/dev/null | head -c 200 || true
        echo ""
        FAIL=$((FAIL + 1))
    fi
}

echo ""
echo "========================================"
echo "  菁选 — 冒烟测试"
echo "========================================"
echo ""

# 登录获取 tokens
ADMIN_TOKEN=$(curl -s "$BASE_URL/auth/login" -X POST -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])" 2>/dev/null || echo "")
sleep 1
STU_TOKEN=$(curl -s "$BASE_URL/auth/login" -X POST -H "Content-Type: application/json" \
    -d '{"username":"2022001","password":"123456"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])" 2>/dev/null || echo "")

echo "【1/6】认证"
check "后端启动"            GET  "/auth/login"            "200|405" "" ""
check "管理端登录"          POST "/auth/login"            "200"     "" '{"username":"admin","password":"admin123"}'
check "错误密码拒绝"        POST "/auth/login"            "401"     "" '{"username":"admin","password":"wrong"}'

echo "【2/6】管理端"
check "仪表盘统计"          GET  "/admin/dashboard/stats" "200" "$ADMIN_TOKEN"
check "审核列表"            GET  "/admin/audit/list"      "200" "$ADMIN_TOKEN"
check "用户列表"            GET  "/admin/users"           "200" "$ADMIN_TOKEN"

echo "【3/6】教师端"
TEA_TOKEN=$(curl -s "$BASE_URL/auth/login" -X POST -H "Content-Type: application/json" \
    -d '{"username":"t001","password":"123456"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])" 2>/dev/null || echo "")
check "待评分作品列表"      GET  "/teacher/work/list"      "200" "$TEA_TOKEN"
check "教师排行榜"          GET  "/teacher/ranking/list"   "200" "$TEA_TOKEN"

echo "【4/6】学生端"
check "我的作品列表"        GET  "/student/works"          "200" "$STU_TOKEN"
check "创建作品"            POST "/student/works"          "200" "$STU_TOKEN" \
    '{"title":"冒烟测试作品","summary":"冒烟测试","techStack":"Java"}'

echo "【5/6】前台"
check "公开作品列表"        GET  "/public/works"           "200" ""
check "公开排行榜"          GET  "/public/ranking/list"    "200" ""

echo "【6/6】安全"
check "越权拦截（学生→教师）" GET "/teacher/work/list"    "403" "$STU_TOKEN"
check "匿名访问拦截"         GET "/student/works"          "401" ""

echo ""
echo "========================================"
echo "  结果: $PASS 通过 / $FAIL 失败"
echo "========================================"
[ "$FAIL" -eq 0 ] && echo "  ✅ 冒烟测试通过" || echo "  ❌ 存在失败项"
echo ""
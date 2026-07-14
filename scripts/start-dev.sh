#!/usr/bin/env bash

set -euo pipefail

skip_install=false
if [[ ${1:-} == "--skip-install" ]]; then
  skip_install=true
elif [[ $# -ne 0 ]]; then
  echo "用法: bash scripts/start-dev.sh [--skip-install]" >&2
  exit 2
fi

project_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
backend_dir="$project_root/backend"
frontend_dir="$project_root/frontend"
log_dir="$project_root/logs/dev"
mkdir -p "$log_dir"

step() { printf '\n==> %s\n' "$1"; }
ok() { printf '[OK] %s\n' "$1"; }
warn() { printf '[WARN] %s\n' "$1" >&2; }
die() { printf '[ERROR] %s\n' "$1" >&2; exit 1; }

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "缺少命令 $1。"
}

port_open() {
  (echo >/dev/tcp/127.0.0.1/"$1") >/dev/null 2>&1
}

redis_ping() {
  local response
  exec 3<>/dev/tcp/127.0.0.1/6379 || return 1
  printf '*1\r\n$4\r\nPING\r\n' >&3
  IFS= read -r -t 3 response <&3 || {
    exec 3>&-
    exec 3<&-
    return 1
  }
  exec 3>&-
  exec 3<&-
  [[ $response == '+PONG' ]]
}

wait_for_port() {
  local port=$1 timeout=${2:-60} elapsed=0
  until port_open "$port"; do
    ((++elapsed))
    ((elapsed >= timeout)) && return 1
    sleep 1
  done
}

wait_for_http() {
  local url=$1 timeout=${2:-120} elapsed=0
  until curl --fail --silent --show-error --max-time 3 "$url" >/dev/null; do
    ((++elapsed))
    ((elapsed >= timeout)) && return 1
    sleep 1
  done
}

hash_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

if [[ -f "$project_root/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$project_root/.env"
  set +a
fi

if [[ -z ${JWT_SECRET:-} ]]; then
  require_command openssl
  JWT_SECRET=$(openssl rand -base64 48 | tr -d '\r\n')
  [[ -n $JWT_SECRET ]] || die "无法生成开发用 JWT 密钥。"
  export JWT_SECRET
  warn "未配置 JWT_SECRET，已生成仅本次启动有效的开发密钥。"
fi

if [[ -z ${DB_PASSWORD:-} ]]; then
  read -r -s -p "未检测到 .env/DB_PASSWORD，请输入本机 MySQL root 密码: " DB_PASSWORD
  printf '\n'
  [[ -n $DB_PASSWORD ]] || die "未提供必需的数据库密码。"
  export DB_PASSWORD
fi

step "检查开发工具"
require_command java
require_command mvn
require_command npm
require_command curl
ok "Java、Maven、Node/npm 均可用"

step "检查基础服务"
if ! port_open 3306; then
  die "MySQL 未在 127.0.0.1:3306 运行。请先启动本机 MySQL。"
fi
ok "MySQL 已在 127.0.0.1:3306 运行"

if ! port_open 6379; then
  require_command redis-server
  warn "Redis 未运行，正在启动本地 Redis。"
  redis-server --daemonize yes --port 6379
  wait_for_port 6379 30 || die "Redis 启动失败，6379 端口未就绪。"
fi
redis_ping || die "Redis PING 未通过。"
ok "Redis 已在 127.0.0.1:6379 运行"

# 开发环境未配置邮件时使用不可投递占位配置，确保应用能启动。
if [[ -z ${MAIL_USERNAME:-} || -z ${MAIL_PASSWORD:-} ]]; then
  export MAIL_HOST=127.0.0.1 MAIL_PORT=1025
  export MAIL_USERNAME=dev-disabled@localhost MAIL_PASSWORD=dev-disabled
  warn "未配置邮件凭据，验证码邮件功能不可用。"
fi

export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/jingxuan?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowMultiQueries=true'
export SPRING_DATA_REDIS_HOST=127.0.0.1
export SPRING_DATA_REDIS_PORT=6379

backend_url=http://127.0.0.1:8080
backend_health_url="$backend_url/public/works?pageNum=1&pageSize=1"

step "启动后端"
if port_open 8080; then
  wait_for_http "$backend_health_url" 5 || die "8080 已被其他服务占用，且菁选后端健康检查未通过。"
  ok "后端已在 8080 运行"
else
  (
    cd "$backend_dir"
    nohup mvn clean spring-boot:run -Dspring-boot.run.profiles=dev >"$log_dir/backend.out.log" 2>"$log_dir/backend.err.log" &
    echo $! >"$log_dir/backend.pid"
  )
  wait_for_http "$backend_health_url" 180 || {
    tail -n 30 "$log_dir/backend.out.log" 2>/dev/null || true
    tail -n 30 "$log_dir/backend.err.log" 2>/dev/null || true
    die "后端启动失败或健康检查超时。"
  }
  ok "后端已启动：$backend_url"
fi

step "准备前端依赖"
package_json="$frontend_dir/package.json"
package_lock="$frontend_dir/package-lock.json"
dependency_stamp="$log_dir/frontend-dependencies.sha256"
fingerprint=$(hash_file "$package_json")
[[ -f $package_lock ]] && fingerprint+=":$(hash_file "$package_lock")"

if [[ ! -x "$frontend_dir/node_modules/.bin/vite" || $(cat "$dependency_stamp" 2>/dev/null || true) != "$fingerprint" ]]; then
  if [[ $skip_install == true ]]; then
    warn "前端依赖缺失或配置已变化，但已按参数跳过安装。"
  elif [[ -f $package_lock ]]; then
    (cd "$frontend_dir" && npm ci --registry=https://registry.npmjs.org)
    printf '%s\n' "$fingerprint" >"$dependency_stamp"
    ok "前端依赖安装完成"
  else
    (cd "$frontend_dir" && npm install --registry=https://registry.npmjs.org)
    printf '%s\n' "$fingerprint" >"$dependency_stamp"
    ok "前端依赖安装完成"
  fi
else
  ok "前端依赖与 package/lock 指纹一致"
fi

frontend_url=http://127.0.0.1:5173/jingxuan/
step "启动前端"
if port_open 5173; then
  wait_for_http "$frontend_url" 5 || die "5173 已被其他服务占用，且菁选前端健康检查未通过。"
  ok "前端已在 5173 运行"
else
  (
    cd "$frontend_dir"
    nohup npm run dev -- --host 127.0.0.1 >"$log_dir/frontend.out.log" 2>"$log_dir/frontend.err.log" &
    echo $! >"$log_dir/frontend.pid"
  )
  wait_for_http "$frontend_url" 120 || {
    tail -n 30 "$log_dir/frontend.out.log" 2>/dev/null || true
    tail -n 30 "$log_dir/frontend.err.log" 2>/dev/null || true
    die "前端启动失败或健康检查超时。"
  }
  ok "前端已启动：$frontend_url"
fi

printf '\n菁选项目启动完成\n前端：%s\n后端：%s\n接口文档：%s/doc.html\n日志目录：%s\n' \
  "$frontend_url" "$backend_url" "$backend_url" "$log_dir"

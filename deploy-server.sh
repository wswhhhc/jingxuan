#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/opt/jingxuan"

cd "$APP_DIR"

if [ ! -f "$APP_DIR/.env" ]; then
  echo "Missing $APP_DIR/.env. Copy .env.example to .env and fill DB_PASSWORD first." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source "$APP_DIR/.env"
set +a

mkdir -p "$APP_DIR/uploads"

pm2 delete jingxuan-back >/dev/null 2>&1 || true
pm2 start "$APP_DIR/ecosystem.config.cjs"
pm2 save

if command -v nginx >/dev/null 2>&1; then
  if [ -d /etc/nginx/conf.d ]; then
    cp "$APP_DIR/nginx-jingxuan.conf" /etc/nginx/conf.d/jingxuan.conf
  elif [ -d /etc/nginx/sites-enabled ]; then
    cp "$APP_DIR/nginx-jingxuan.conf" /etc/nginx/sites-enabled/jingxuan.conf
  fi
  nginx -t
  systemctl reload nginx || nginx -s reload
fi

echo "Backend: http://127.0.0.1:8080/doc.html"
echo "Frontend: http://SERVER_IP/"


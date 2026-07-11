#!/bin/bash
# ============================================================
# Docker 环境数据库初始化脚本
# 在 MySQL 容器首次启动时按顺序执行所有 SQL 文件
# ============================================================

set -e

echo "=== 开始初始化数据库 ==="

# 先执行基础建表脚本
echo ">>> 执行基础建表: base/init_schema.sql"
mysql -u root -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < /docker-entrypoint-initdb.d/base/init_schema.sql

# 按文件名排序执行业务迁移脚本
for f in $(ls /docker-entrypoint-initdb.d/business/*.sql 2>/dev/null | sort); do
  echo ">>> 执行迁移: $(basename $f)"
  mysql -u root -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "$f"
done

echo "=== 数据库初始化完成 ==="

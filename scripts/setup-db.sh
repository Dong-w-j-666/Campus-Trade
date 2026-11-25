#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CONFIG_FILE="$ROOT_DIR/db/db.properties"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "未找到 db/db.properties。请先执行："
  echo "cp db/db.properties.example db/db.properties"
  exit 1
fi

read_property() {
  sed -n "s/^$1=//p" "$CONFIG_FILE" | head -n 1
}

db_user="$(read_property db.user)"
db_password="$(read_property db.password)"
mysql_host="${MYSQL_HOST:-127.0.0.1}"
mysql_port="${MYSQL_PORT:-3306}"

echo "将重建本机 campus_trade 数据库，现有同名表会被删除。"
echo "目标：$mysql_host:$mysql_port / 用户：$db_user"
read -r -p "输入 yes 继续：" confirmation
if [[ "$confirmation" != "yes" ]]; then
  echo "已取消。"
  exit 0
fi

export MYSQL_PWD="$db_password"
mysql --protocol=tcp -h "$mysql_host" -P "$mysql_port" -u "$db_user" < "$ROOT_DIR/db/schema.sql"
mysql --protocol=tcp -h "$mysql_host" -P "$mysql_port" -u "$db_user" < "$ROOT_DIR/db/data.sql"
unset MYSQL_PWD

echo "数据库已初始化。"

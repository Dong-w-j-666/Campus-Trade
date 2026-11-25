#!/usr/bin/env bash
set -euo pipefail

output_file="${1:-.env.production}"

if [[ -e "$output_file" ]]; then
  echo "配置文件已存在：$output_file"
  echo "为避免覆盖已有数据库密码，请先备份或移走该文件。"
  exit 1
fi

if ! command -v openssl >/dev/null 2>&1; then
  echo "未找到 openssl，请先安装后重试。"
  exit 1
fi

mysql_root_password="$(openssl rand -hex 24)"
db_password="$(openssl rand -hex 24)"
app_auth_secret="$(openssl rand -hex 32)"

umask 077
cat >"$output_file" <<EOF
MYSQL_ROOT_PASSWORD=$mysql_root_password
MYSQL_DATABASE=campus_trade
DB_USERNAME=campus_app
DB_PASSWORD=$db_password
APP_AUTH_SECRET=$app_auth_secret
APP_BIND_ADDRESS=0.0.0.0
APP_HOST_PORT=8080
APP_SECURE_COOKIE=false
EOF
chmod 600 "$output_file"

echo "已生成 ${output_file}，文件权限为 600。"
echo "密码仅保存在服务器本地，没有输出到终端。"

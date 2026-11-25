#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if command -v pnpm >/dev/null 2>&1; then
  PACKAGE_MANAGER=(pnpm)
elif command -v npm >/dev/null 2>&1; then
  PACKAGE_MANAGER=(npm)
else
  echo "未找到 pnpm 或 npm，请先安装 Node.js 20 及以上版本。"
  exit 1
fi

cd "$ROOT_DIR/web"
"${PACKAGE_MANAGER[@]}" install
"${PACKAGE_MANAGER[@]}" run build

mkdir -p "$ROOT_DIR/server/src/main/resources/static"
cp -R "$ROOT_DIR/web/dist/." "$ROOT_DIR/server/src/main/resources/static/"

cd "$ROOT_DIR/server"
mvn -DskipTests package

echo
echo "构建完成：server/target/campus-trade-server-0.1.0.jar"

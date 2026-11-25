#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAR_FILE="$ROOT_DIR/server/target/campus-trade-server-0.1.0.jar"
CONFIG_FILE="$ROOT_DIR/db/db.properties"

cd "$ROOT_DIR"

if [[ ! -f "$JAR_FILE" ]]; then
  echo "首次运行，正在构建后端……"
  mvn -f server/pom.xml -DskipTests package
fi

if [[ -f "$CONFIG_FILE" ]]; then
  export DB_USERNAME="${DB_USERNAME:-$(sed -n 's/^db.user=//p' "$CONFIG_FILE" | head -n 1)}"
  export DB_PASSWORD="${DB_PASSWORD:-$(sed -n 's/^db.password=//p' "$CONFIG_FILE" | head -n 1)}"
fi

echo "打开 http://localhost:8080"
exec java -jar "$JAR_FILE"

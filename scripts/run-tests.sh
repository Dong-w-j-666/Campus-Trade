#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/out/production/Campus-Trade"
JDBC_JAR="$ROOT_DIR/lib/mysql-connector-j-8.0.31.jar"

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
  JAVAC="$JAVA_HOME/bin/javac"
  JAVA="$JAVA_HOME/bin/java"
elif command -v javac >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
  JAVAC="$(command -v javac)"
  JAVA="$(command -v java)"
else
  echo "未找到 Java 21 或更高版本，请先安装 JDK 并配置 JAVA_HOME。"
  exit 1
fi

WITH_DB=0
RUN_LEGACY=1
for arg in "$@"; do
  case "$arg" in
    --with-db) WITH_DB=1 ;;
    --no-legacy) RUN_LEGACY=0 ;;
    *)
      echo "用法：./scripts/run-tests.sh [--with-db] [--no-legacy]"
      exit 2
      ;;
  esac
done

cd "$ROOT_DIR"
rm -rf -- "$OUT_DIR"
mkdir -p "$OUT_DIR"

echo "使用 $("$JAVAC" -version 2>&1) 编译，目标字节码版本为 Java 21。"
"$JAVAC" -encoding UTF-8 --release 21 -d "$OUT_DIR" \
  src/CampusMarketTest.java src/Experiment8Test.java
"$JAVAC" -encoding UTF-8 --release 21 -cp "$JDBC_JAR" -d "$OUT_DIR" \
  src/DatabaseSmokeTest.java

"$JAVA" -cp "$OUT_DIR" CampusMarketTest
if [[ "$RUN_LEGACY" -eq 1 ]]; then
  "$JAVA" -cp "$OUT_DIR" CampusMarketTest --legacy
fi
"$JAVA" -cp "$OUT_DIR" Experiment8Test

if [[ "$WITH_DB" -eq 1 ]]; then
  "$JAVA" -cp "$OUT_DIR:$JDBC_JAR" DatabaseSmokeTest
else
  echo
  echo "数据库校验未执行。启动 MySQL 并导入 db/schema.sql、db/data.sql 后，"
  echo "运行 ./scripts/run-tests.sh --with-db 进行验证。"
fi

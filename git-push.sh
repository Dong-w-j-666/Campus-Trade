#!/bin/bash
# 一键：初始化本地仓库 → 提交 → 推送到 GitHub
# 用法：./git-push.sh [仓库地址]

set -e
cd "$(dirname "$0")" || exit 1

NAME="董文杰"
EMAIL="2436100035@users.noreply.github.com"
MSG="实验一至实验三：选题、需求分析、软件设计、软件测试与数据库"

if ! command -v git >/dev/null 2>&1; then
  echo "未检测到 git，请先安装 Xcode Command Line Tools：xcode-select --install"
  exit 1
fi

if [ ! -d .git ]; then
  git init -q
  git symbolic-ref HEAD refs/heads/main
  echo "已初始化本地仓库（main 分支）"
fi

git config user.name "$NAME"
git config user.email "$EMAIL"

git add -A
if git diff --cached --quiet; then
  echo "没有新的改动需要提交"
else
  git commit -q -m "$MSG"
  echo "已提交：$(git log --oneline -1)"
fi

URL="$1"
if [ -z "$URL" ]; then
  echo
  echo "请先在 https://github.com/new 新建空仓库（建议 Private，不要勾选 README/.gitignore）"
  read -r -p "粘贴仓库地址（https://github.com/用户名/Campus-Trade.git）: " URL
fi

if [ -z "$URL" ]; then
  echo "未提供仓库地址，已停止。本地提交已完成，之后可手动 git remote add origin <url> 并 git push -u origin main"
  exit 0
fi

git remote remove origin 2>/dev/null || true
git remote add origin "$URL"
git push -u origin main

echo
echo "推送完成。请在 GitHub 刷新页面查看。"

#!/bin/bash
# 提交并推送 Campus-Trade。
# 用法：./git-push.sh [仓库地址] [提交消息]

set -e
cd "$(dirname "$0")" || exit 1

NAME="${GIT_AUTHOR_NAME:-董文杰}"
EMAIL="${GIT_AUTHOR_EMAIL:-2436100035@users.noreply.github.com}"
DEFAULT_MSG="更新 Campus-Trade 项目 $(date '+%Y-%m-%d %H:%M')"
MSG="${2:-$DEFAULT_MSG}"

if ! command -v git >/dev/null 2>&1; then
  echo "未检测到 git，请先安装 Xcode Command Line Tools：xcode-select --install"
  exit 1
fi

if [ ! -d .git ]; then
  git init -q
  git symbolic-ref HEAD refs/heads/main
  echo "已初始化本地仓库（main 分支）"
fi

if [ -z "$(git config user.name || true)" ]; then
  git config user.name "$NAME"
fi
if [ -z "$(git config user.email || true)" ]; then
  git config user.email "$EMAIL"
fi

git add -A
if git diff --cached --quiet; then
  echo "没有新的改动需要提交"
else
  git commit -q -m "$MSG"
  echo "已提交：$(git log --oneline -1)"
fi

URL="$1"
if [ -n "$URL" ]; then
  if git remote get-url origin >/dev/null 2>&1; then
    git remote set-url origin "$URL"
  else
    git remote add origin "$URL"
  fi
elif ! git remote get-url origin >/dev/null 2>&1; then
  echo
  echo "请先在 https://github.com/new 新建空仓库"
  read -r -p "粘贴仓库地址（https://github.com/用户名/Campus-Trade.git）: " URL
  if [ -z "$URL" ]; then
    echo "未提供仓库地址，已停止。"
    exit 1
  fi
  git remote add origin "$URL"
fi

git push -u origin main

echo
echo "推送完成。请在 GitHub 刷新页面查看。"

#!/usr/bin/env bash
# Chạy khi kết thúc phiên (SessionEnd). Stage mọi thay đổi, commit nếu có,
# rồi push HEAD lên origin. Không bao giờ làm hỏng việc kết thúc phiên.

cd "$(git rev-parse --show-toplevel 2>/dev/null)" || exit 0

git add -A

if ! git diff --cached --quiet; then
  git commit -q -m "chore: auto-commit session $(date '+%Y-%m-%d %H:%M')" || exit 0
fi

# GIT_TERMINAL_PROMPT=0: không chờ nhập mật khẩu nếu credential hết hạn.
GIT_TERMINAL_PROMPT=0 git push -q origin HEAD 2>&1 \
  || echo "auto-commit: push thất bại (không chặn phiên)." >&2

exit 0

#!/usr/bin/env bash
# ============================================================================
# 回滚 · 乡野智游
# 用法：
#   sudo bash deploy/scripts/rollback.sh              # 回到上一个发布包
#   sudo bash deploy/scripts/rollback.sh --list       # 列出可用发布包
#   sudo bash deploy/scripts/rollback.sh --to 20261118-1600
#
# 回滚范围：只切后端 jar 与前端静态资源，不动数据库。
#           若本次发布含数据库结构变更，必须另外执行备份文件恢复（见 DEPLOY.md）。
# ============================================================================
set -euo pipefail

APP_ROOT="${APP_ROOT:-/opt/rural-travel}"
SERVICE="${SERVICE:-rural-travel}"
RELEASES="$APP_ROOT/releases"
TARGET=""

# 逐键读取 .env，不 source：jdbc url 含 & ，source 会被当作命令分隔符
env_get() {
  local key="$1" line
  line=$(grep -m1 "^${key}=" "$APP_ROOT/.env" 2>/dev/null) || return 0
  line=${line#*=}
  line=${line%\"}; line=${line#\"}
  line=${line%\'}; line=${line#\'}
  printf '%s' "$line"
}

while [ $# -gt 0 ]; do
  case "$1" in
    --list)
      echo "可用发布包（新 → 旧）："
      # shellcheck disable=SC2012
      ls -1dt "$RELEASES"/*/ 2>/dev/null | while read -r d; do
        cur=""
        [ "$(readlink -f "$APP_ROOT/app.jar" 2>/dev/null)" = "$(readlink -f "$d/app.jar")" ] && cur="  <= 当前"
        printf "  %s%s\n" "$(basename "$d")" "$cur"
      done
      exit 0 ;;
    --to) TARGET="${2:-}"; shift 2 ;;
    -h|--help) sed -n '2,12p' "$0"; exit 0 ;;
    *) echo "未知参数: $1"; exit 2 ;;
  esac
done

[ "$(id -u)" -eq 0 ] || { echo "请用 sudo 运行"; exit 1; }

CUR_NAME=$(basename "$(dirname "$(readlink -f "$APP_ROOT/app.jar" 2>/dev/null)")" 2>/dev/null || echo "")
if [ -z "$TARGET" ]; then
  # shellcheck disable=SC2012
  TARGET=$(ls -1dt "$RELEASES"/*/ 2>/dev/null | xargs -r -n1 basename | grep -vx "$CUR_NAME" | head -1 || true)
fi
[ -n "$TARGET" ] || { echo "找不到可回滚的发布包，用 --list 查看"; exit 2; }

REL="$RELEASES/$TARGET"
[ -f "$REL/app.jar" ] && [ -f "$REL/www/index.html" ] || { echo "发布包不完整: $REL"; exit 2; }
[ "$TARGET" = "$CUR_NAME" ] && { echo "$TARGET 已是当前版本"; exit 0; }

echo "当前版本: ${CUR_NAME:-<无>}"
echo "回滚目标: $TARGET"

BACKUP="$APP_ROOT/backups/rollback-$(date +%Y%m%d-%H%M%S)-from-${CUR_NAME:-none}.tar.gz"
tar -czf "$BACKUP" -C "$APP_ROOT" --dereference app.jar www 2>/dev/null || true
echo "已备份当前版本到: $BACKUP"

ln -sfn "$REL/app.jar" "$APP_ROOT/app.jar"
ln -sfn "$REL/www" "$APP_ROOT/www"
systemctl restart "$SERVICE"

# shellcheck disable=SC1091
PORT=$(env_get BACKEND_PORT); PORT="${PORT:-8091}"
OK=0
for i in $(seq 1 45); do
  if curl -fsS --max-time 3 "http://127.0.0.1:$PORT/api/categories" >/dev/null 2>&1; then OK=1; break; fi
  sleep 2
done

if [ "$OK" -eq 1 ]; then
  echo "回滚完成，服务健康（版本 $TARGET）"
else
  echo "[失败] 回滚后健康检查仍未通过，请查 journalctl -u $SERVICE -n 100"
  exit 4
fi

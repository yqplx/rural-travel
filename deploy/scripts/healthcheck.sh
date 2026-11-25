#!/usr/bin/env bash
# ============================================================================
# 健康巡检 + 告警 · 乡野智游
# 用法：bash deploy/scripts/healthcheck.sh
# 退出码：0 正常 / 1 探测失败 / 2 服务异常 / 3 磁盘告警 / 4 日志错误激增
# 定时：*/5 * * * * /usr/bin/bash /opt/rural-travel/deploy/scripts/healthcheck.sh >> /var/log/rural-travel/health.log 2>&1
#
# 告警：设置 WEBHOOK_URL（企业微信/钉钉机器人地址）后，异常时推送一条文本消息。
#       也可用 NOTIFY_CMD="mail -s alert you@example.com" 之类自定义。
# ============================================================================
set -uo pipefail

APP_ROOT="${APP_ROOT:-/opt/rural-travel}"
SERVICE="${SERVICE:-rural-travel}"
WEBHOOK_URL="${WEBHOOK_URL:-}"
LOG_DIR="${LOG_DIR:-/var/log/rural-travel}"
DISK_LIMIT="${DISK_LIMIT:-85}"
ERR_LIMIT="${ERR_LIMIT:-20}"   # 最近 10 分钟 ERROR 条数阈值
STAMP=$(date '+%F %T')

# 逐键读取 .env，不 source：jdbc url 含 & ，source 会被当作命令分隔符
env_get() {
  local key="$1" line
  line=$(grep -m1 "^${key}=" "$APP_ROOT/.env" 2>/dev/null) || return 0
  line=${line#*=}
  line=${line%\"}; line=${line#\"}
  line=${line%\'}; line=${line#\'}
  printf '%s' "$line"
}

CODE=0
MSGS=()

add() { MSGS+=("$1"); }

notify() {
  local text="$1"
  if [ -n "$WEBHOOK_URL" ]; then
    # 企业微信 / 钉钉 通用文本消息体
    curl -fsS -m 5 -H 'Content-Type: application/json' \
      -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"[乡野智游] $text\"}}" \
      "$WEBHOOK_URL" >/dev/null 2>&1 || true
  fi
}

# ---- 1. 进程与端口 ----
if systemctl is-active --quiet "$SERVICE"; then
  add "服务运行中"
else
  add "服务未运行（systemctl status $SERVICE）"; CODE=2
fi

PORT=$(env_get BACKEND_PORT); PORT="${PORT:-8091}"
if curl -fsS --max-time 5 "http://127.0.0.1:$PORT/api/categories" -o /dev/null; then
  add "接口 /api/categories 正常"
else
  add "接口 /api/categories 探测失败"; CODE=2
fi

# ---- 2. 数据库 ----
if command -v mysql >/dev/null 2>&1; then
  if MYSQL_PWD="$(env_get DB_PASS)" mysql -h 127.0.0.1 -P 3306 -u "$(env_get DB_USER)" -e "SELECT 1" >/dev/null 2>&1; then
    add "MySQL 连接正常"
  else
    add "MySQL 连接失败"; CODE=2
  fi
fi

# ---- 3. 磁盘 ----
USE=$(df -P "$APP_ROOT" | awk 'NR==2 {gsub("%","",$5); print $5}')
if [ "${USE:-0}" -ge "$DISK_LIMIT" ]; then
  add "磁盘使用率 ${USE}% 超过阈值 ${DISK_LIMIT}%"; CODE=3
fi

# ---- 4. 日志错误激增 ----
if [ -f "$LOG_DIR/app.log" ]; then
  ERRS=$(grep -c ' ERROR ' "$LOG_DIR/app.log" 2>/dev/null || true)
  if [ "${ERRS:-0}" -ge "$ERR_LIMIT" ]; then
    add "app.log 中 ERROR 累计 ${ERRS} 条（阈值 ${ERR_LIMIT}），请检查"; CODE=4
  fi
fi

# ---- 5. 备份新鲜度 ----
LATEST=$(find "$APP_ROOT/backups" -name 'rural_travel-*.sql.gz' -mtime -2 2>/dev/null | head -1)
if [ -z "$LATEST" ]; then
  add "近 48 小时无数据库备份，检查 cron 是否生效"
  [ "$CODE" -eq 0 ] && CODE=1
fi

echo "[$STAMP] code=$CODE | ${MSGS[*]}"
if [ "$CODE" -ne 0 ]; then
  notify "$STAMP 异常(code=$CODE): ${MSGS[*]}"
fi
exit "$CODE"

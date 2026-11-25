#!/usr/bin/env bash
# ============================================================================
# 数据库备份 · 乡野智游
# 用法：sudo bash deploy/scripts/backup-db.sh [保留天数，默认 7]
# 建议：crontab 每日 03:30 执行
#   30 3 * * * /usr/bin/bash /opt/rural-travel/deploy/scripts/backup-db.sh 7 >> /var/log/rural-travel/backup.log 2>&1
# ============================================================================
set -euo pipefail

APP_ROOT="${APP_ROOT:-/opt/rural-travel}"
KEEP_DAYS="${1:-7}"
ENV_FILE="$APP_ROOT/.env"
DIR="$APP_ROOT/backups"
STAMP=$(date +%Y%m%d-%H%M%S)
OUT="$DIR/rural_travel-$STAMP.sql.gz"

[ -f "$ENV_FILE" ] || { echo "缺少 $ENV_FILE"; exit 1; }

# 逐键读取 .env，不 source：jdbc url 含 & ，source 会被当作命令分隔符导致值被截断
env_get() {
  local key="$1" line
  line=$(grep -m1 "^${key}=" "$ENV_FILE" 2>/dev/null) || return 0
  line=${line#*=}
  line=${line%\"}; line=${line#\"}
  line=${line%\'}; line=${line#\'}
  printf '%s' "$line"
}

DB_USER=$(env_get DB_USER)
DB_PASS=$(env_get DB_PASS)
SPRING_DATASOURCE_URL=$(env_get SPRING_DATASOURCE_URL)
: "${DB_USER:?DB_USER 未设置}"
: "${DB_PASS:?DB_PASS 未设置}"

# 从 jdbc url 里解析主机与端口（默认 127.0.0.1:3306）
HOST=$(printf '%s' "${SPRING_DATASOURCE_URL:-}" | sed -n 's#.*//\([^:/]*\).*#\1#p'); HOST="${HOST:-127.0.0.1}"
PORT=$(printf '%s' "${SPRING_DATASOURCE_URL:-}" | sed -n 's#.*//[^:/]*:\([0-9]*\)/.*#\1#p'); PORT="${PORT:-3306}"
DBNAME="rural_travel"

mkdir -p "$DIR"

echo "[$(date '+%F %T')] 开始备份 $DBNAME@$HOST:$PORT"
# --single-transaction 保证 InnoDB 一致性且不锁表；--routines --triggers 带上存储过程
if ! MYSQL_PWD="$DB_PASS" mysqldump \
      -h "$HOST" -P "$PORT" -u "$DB_USER" \
      --single-transaction --quick --routines --triggers \
      --default-character-set=utf8mb4 --set-gtid-purged=OFF \
      --databases "$DBNAME" 2>/dev/null | gzip -6 > "$OUT"; then
  echo "[$(date '+%F %T')] [失败] mysqldump 执行失败"; rm -f "$OUT"; exit 1
fi

# 校验：gzip 完整性 + 体积下限（空库也有几 KB 结构）
if ! gzip -t "$OUT"; then echo "[失败] 备份文件损坏: $OUT"; exit 1; fi
SIZE=$(stat -c '%s' "$OUT")
if [ "$SIZE" -lt 10240 ]; then echo "[失败] 备份过小（${SIZE}B），可能不是有效备份"; exit 1; fi

# 顺手记一下表数量，便于和预期对比
TBL=$(gzip -dc "$OUT" | grep -c '^CREATE TABLE' || true)
echo "[$(date '+%F %T')] 完成：$OUT（$((SIZE/1024))KB，$TBL 张表）"

echo "清理 ${KEEP_DAYS} 天前的备份..."
find "$DIR" -name 'rural_travel-*.sql.gz' -mtime "+$KEEP_DAYS" -print -delete | sed 's/^/  已删除 /'

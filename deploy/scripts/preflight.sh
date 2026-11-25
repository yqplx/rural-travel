#!/usr/bin/env bash
# ============================================================================
# 上线前自检 · 乡野智游
# 用法：sudo bash deploy/scripts/preflight.sh
# 作用：只读检查，不改动任何东西。全部 PASS 才建议部署。
# ============================================================================
set -uo pipefail

APP_ROOT="${APP_ROOT:-/opt/rural-travel}"
SERVICE="${SERVICE:-rural-travel}"
ENV_FILE="$APP_ROOT/.env"
FAIL=0

# 逐键读取 .env，绝不 source：
# jdbc url 里含 & ，source 会被 shell 当成命令分隔符，导致变量被截断成空值
env_get() {
  local key="$1" line
  line=$(grep -m1 "^${key}=" "$ENV_FILE" 2>/dev/null) || return 0
  line=${line#*=}
  line=${line%\"}; line=${line#\"}
  line=${line%\'}; line=${line#\'}
  printf '%s' "$line"
}

pass() { printf '  [PASS] %s\n' "$1"; }
warn() { printf '  [WARN] %s\n' "$1"; }
fail() { printf '  [FAIL] %s\n' "$1"; FAIL=$((FAIL+1)); }

echo "== 1. 运行时 =="
if command -v java >/dev/null 2>&1; then
  JV=$(java -version 2>&1 | head -1)
  case "$JV" in
    *'"17.'*|*'"21.'*) pass "Java: $JV" ;;
    *) fail "Java 版本应为 17 或 21，当前: $JV" ;;
  esac
else
  fail "未找到 java，请安装 OpenJDK 17（systemd 读取 /usr/bin/java）"
fi

for c in nginx mysqldump curl rsync openssl; do
  command -v "$c" >/dev/null 2>&1 && pass "命令可用: $c" || fail "缺少命令: $c"
done

echo "== 2. 目录与权限 =="
if [ -f "$ENV_FILE" ]; then
  pass "环境变量文件存在: $ENV_FILE"
  MODE=$(stat -c '%a' "$ENV_FILE")
  [ "$MODE" = "600" ] && pass "权限 600" || warn "权限为 $MODE，建议 chmod 600（含明文口令）"
  if grep -qE 'CHANGE_ME' "$ENV_FILE"; then
    fail "环境变量中仍有 CHANGE_ME 占位符未替换（grep -n CHANGE_ME $ENV_FILE）"
  else
    pass "无未替换的占位符"
  fi
  # 用 env_get 取值，不 source（见文件顶部说明）
  JWT_SECRET=$(env_get JWT_SECRET)
  AI_CRYPTO_SECRET=$(env_get AI_CRYPTO_SECRET)
  SITE_BASE_URL=$(env_get SITE_BASE_URL)
  DB_USER=$(env_get DB_USER)
  DB_PASS=$(env_get DB_PASS)
  SPRING_DATASOURCE_URL=$(env_get SPRING_DATASOURCE_URL)
  for v in JWT_SECRET AI_CRYPTO_SECRET SITE_BASE_URL DB_USER DB_PASS SPRING_DATASOURCE_URL; do
    [ -n "${!v:-}" ] && pass "变量已设置: $v" || fail "变量缺失: $v"
  done
  if [ "${#JWT_SECRET:-0}" -lt 32 ]; then
    fail "JWT_SECRET 长度 ${#JWT_SECRET} < 32，强度不足"
  else
    pass "JWT_SECRET 长度 ${#JWT_SECRET}"
  fi
  if [ "${JWT_SECRET:-}" = "rural-travel-jwt-secret-key-2026-do-not-leak" ]; then
    fail "JWT_SECRET 仍是源码里的公开默认值，等于未加密，可伪造任意用户 token"
  fi
  if [ "${AI_CRYPTO_SECRET:-}" = "CHANGE_ME_or_keep_rural-travel-ai-crypto-2026" ]; then
    warn "AI_CRYPTO_SECRET 未定：沿用旧值=已存 Key 可解；换新值=用户需重填 AI Key"
  fi
else
  fail "缺少 $ENV_FILE（从 deploy/env.prod.example 复制）"
fi

if [ -d "$APP_ROOT" ]; then
  pass "应用目录存在: $APP_ROOT"
else
  fail "应用目录不存在: $APP_ROOT（先跑 deploy.sh）"
fi
if [ -f "$APP_ROOT/config/application-prod.yml" ]; then
  pass "外置生产配置存在"
else
  fail "缺少 $APP_ROOT/config/application-prod.yml"
fi
if [ -e "$APP_ROOT/app.jar" ] && [ -e "$APP_ROOT/www/index.html" ]; then
  pass "app.jar 与 www 已就位"
else
  warn "app.jar 或 www 尚未就位（首次部署由 deploy.sh 创建）"
fi
if mkdir -p /var/log/rural-travel 2>/dev/null && [ -w /var/log/rural-travel ]; then
  pass "日志目录可写: /var/log/rural-travel"
else
  fail "日志目录不可写: /var/log/rural-travel（chown rural:rural）"
fi

echo "== 3. 数据库 =="
if [ -n "${SPRING_DATASOURCE_URL:-}" ] && [ -n "${DB_USER:-}" ]; then
  if MYSQL_PWD="${DB_PASS:-}" mysql -h 127.0.0.1 -P 3306 -u "$DB_USER" -e "SELECT 1" >/dev/null 2>&1; then
    pass "MySQL 连接成功"
    TB=$(MYSQL_PWD="${DB_PASS:-}" mysql -h 127.0.0.1 -P 3306 -u "$DB_USER" -N -B \
          -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='rural_travel'" 2>/dev/null)
    [ "${TB:-0}" -ge 20 ] && pass "rural_travel 表数量: $TB" \
                          || warn "rural_travel 表数量仅 ${TB:-0}，可能未导入数据"
  else
    fail "MySQL 连接失败（检查服务、账号、库 rural_travel）"
  fi
fi

echo "== 4. 端口与资源 =="
for p in 80 443 8091; do
  if ss -ltn "sport = :$p" 2>/dev/null | grep -q LISTEN; then
    pass "端口 $p 已监听"
  else
    [ "$p" = "8091" ] && warn "端口 $p 未监听（后端未启动，属正常）" || warn "端口 $p 未监听"
  fi
done
USE=$(df -P "$APP_ROOT" 2>/dev/null | awk 'NR==2 {gsub("%","",$5); print $5}')
if [ -n "${USE:-}" ]; then
  [ "$USE" -lt 85 ] && pass "磁盘使用率 ${USE}%" || fail "磁盘使用率 ${USE}%，低于 15% 余量不建议上线"
fi
MEM=$(awk '/MemAvailable/ {printf "%d", $2/1024}' /proc/meminfo 2>/dev/null)
[ -n "${MEM:-}" ] && { [ "$MEM" -gt 800 ] && pass "可用内存 ${MEM}MB" || warn "可用内存仅 ${MEM}MB，1G 堆可能吃紧"; }

echo "== 5. Nginx =="
if nginx -t >/dev/null 2>&1; then pass "nginx -t 通过"; else fail "nginx -t 失败，先修配置"; fi

echo
if [ "$FAIL" -eq 0 ]; then
  echo "结果：全部通过，可以部署。"
  exit 0
else
  echo "结果：$FAIL 项失败，请先处理后部署。"
  exit 1
fi

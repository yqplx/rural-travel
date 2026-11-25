#!/usr/bin/env bash
# ============================================================================
# 部署 / 发版 · 乡野智游（幂等，可重复执行）
# 用法：
#   sudo bash deploy/scripts/deploy.sh --jar ./rural-travel-backend.jar --www ./dist
#   sudo bash deploy/scripts/deploy.sh --jar ... --www ... --no-nginx
#
# 做的事：建用户名 → 建发布目录 → 拷贝产物 → 原子切换软链 → 重启 → 健康检查
#        失败自动回退到上一个版本（不改数据库，不动数据文件）
# ============================================================================
set -euo pipefail

APP_ROOT="${APP_ROOT:-/opt/rural-travel}"
APP_USER="${APP_USER:-rural}"
SERVICE="${SERVICE:-rural-travel}"
KEEP_RELEASES="${KEEP_RELEASES:-5}"
JAR_SRC=""; WWW_SRC=""; DO_NGINX=1

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
    --jar) JAR_SRC="${2:-}"; shift 2 ;;
    --www) WWW_SRC="${2:-}"; shift 2 ;;
    --no-nginx) DO_NGINX=0; shift ;;
    -h|--help) sed -n '2,12p' "$0"; exit 0 ;;
    *) echo "未知参数: $1"; exit 2 ;;
  esac
done

[ "$(id -u)" -eq 0 ] || { echo "请用 sudo 运行"; exit 1; }
[ -n "$JAR_SRC" ] && [ -f "$JAR_SRC" ] || { echo "缺少或找不到 --jar 文件"; exit 2; }
[ -n "$WWW_SRC" ] && [ -f "$WWW_SRC/index.html" ] || { echo "--www 必须指向含 index.html 的构建目录"; exit 2; }

RELEASES="$APP_ROOT/releases"
TS=$(date +%Y%m%d-%H%M%S)
REL="$RELEASES/$TS"

echo "== 1. 准备用户与目录 =="
if ! id "$APP_USER" >/dev/null 2>&1; then
  useradd --system --shell /usr/sbin/nologin --home-dir "$APP_ROOT" "$APP_USER"
  echo "  已创建系统用户 $APP_USER"
fi
mkdir -p "$RELEASES" "$APP_ROOT/config" "$APP_ROOT/backups" /var/log/rural-travel
chown -R "$APP_USER:$APP_USER" /var/log/rural-travel
chmod 750 /var/log/rural-travel

if [ ! -f "$APP_ROOT/config/application-prod.yml" ]; then
  if [ -f "deploy/config/application-prod.yml" ]; then
    cp "deploy/config/application-prod.yml" "$APP_ROOT/config/"
    echo "  已安装外置生产配置"
  else
    echo "  [警告] 未找到 application-prod.yml，后端将使用 jar 内默认配置"
  fi
fi

if [ ! -f "$APP_ROOT/.env" ]; then
  echo "  [阻断] 缺少 $APP_ROOT/.env"
  echo "         请先: cp deploy/env.prod.example $APP_ROOT/.env && 编辑后 chmod 600"
  exit 3
fi

echo "== 2. 生成发布包 $TS =="
mkdir -p "$REL/www"
cp "$JAR_SRC" "$REL/app.jar"
cp -a "$WWW_SRC/." "$REL/www/"
chown -R "$APP_USER:$APP_USER" "$REL"
chmod 640 "$REL/app.jar"
du -sh "$REL" | awk '{print "  发布包体积: "$1}'

echo "== 3. 记录上一版本 =="
PREV_JAR=""; PREV_WWW=""
[ -L "$APP_ROOT/app.jar" ] && PREV_JAR=$(readlink -f "$APP_ROOT/app.jar" || true)
[ -L "$APP_ROOT/www" ] && PREV_WWW=$(readlink -f "$APP_ROOT/www" || true)
echo "  app.jar -> ${PREV_JAR:-<无>}"
echo "  www     -> ${PREV_WWW:-<无>}"

echo "== 4. 切换软链 =="
ln -sfn "$REL/app.jar" "$APP_ROOT/app.jar"
ln -sfn "$REL/www" "$APP_ROOT/www"

echo "== 5. 重启服务 =="
systemctl daemon-reload
systemctl enable "$SERVICE" >/dev/null 2>&1 || true
systemctl restart "$SERVICE"

echo "== 6. 健康检查（最多 90s）=="
PORT=$(env_get BACKEND_PORT); PORT="${PORT:-8091}"
OK=0
for i in $(seq 1 45); do
  if curl -fsS --max-time 3 "http://127.0.0.1:$PORT/api/categories" >/dev/null 2>&1; then
    OK=1; echo "  第 $((i*2))s 依赖就绪"; break
  fi
  sleep 2
done

if [ "$OK" -ne 1 ]; then
  echo "  [失败] 健康检查未通过，开始回退"
  if [ -n "$PREV_JAR" ]; then ln -sfn "$PREV_JAR" "$APP_ROOT/app.jar"; fi
  if [ -n "$PREV_WWW" ]; then ln -sfn "$PREV_WWW" "$APP_ROOT/www"; fi
  systemctl restart "$SERVICE" || true
  echo "  ---- 最近 60 行日志 ----"
  journalctl -u "$SERVICE" -n 60 --no-pager || true
  echo "  已回退到上一版本，最新发布包保留在 $REL 供排查"
  exit 4
fi

echo "== 7. 重载 Nginx =="
if [ "$DO_NGINX" -eq 1 ] && command -v nginx >/dev/null 2>&1; then
  nginx -t && systemctl reload nginx && echo "  Nginx 已重载" || echo "  [警告] Nginx 重载失败，请手工检查"
fi

echo "== 8. 清理旧发布包（保留 $KEEP_RELEASES 个）=="
# shellcheck disable=SC2012
ls -1dt "$RELEASES"/*/ 2>/dev/null | tail -n "+$((KEEP_RELEASES+1))" | while read -r d; do
  case "$(readlink -f "$APP_ROOT/app.jar")" in "$d"*) continue ;; esac
  rm -rf "$d" && echo "  已删除 $d"
done

echo
echo "部署完成：$REL"
echo "验证：curl -fsS http://127.0.0.1:$PORT/api/categories | head -c 200"
echo "回滚：sudo bash deploy/scripts/rollback.sh"

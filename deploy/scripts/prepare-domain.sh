#!/usr/bin/env bash
# ============================================================
# 乡野智游 —— 备案前一次性准备（幂等，可重复执行）
#
# 做三件事：
#   1. 安装域名版 Nginx 配置（域名 + IP 并存，另开 8080 供备案前自测）
#   2. 安装登录限流配置
#   3. 把 SITE_BASE_URL 固化为 http://127.0.0.1
#      —— 该变量只用于后端抓自己的图算 pHash，走回环可彻底摆脱
#         域名解析、HTTPS 证书、公网回环三重依赖
#
# 改动前自动备份，失败即回滚 Nginx 配置。
# 用法：sudo bash prepare-domain.sh [域名]
# ============================================================
set -euo pipefail

DOMAIN="${1:-xiangyezhiyou.cn}"
APP_ROOT="/opt/rural-travel"
ENV_FILE="$APP_ROOT/.env"
SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/nginx"
TS=$(date +%Y%m%d-%H%M%S)

log()  { printf "\033[36m==>\033[0m %s\n" "$1"; }
ok()   { printf "  \033[32m[OK]\033[0m %s\n" "$1"; }
die()  { printf "  \033[31m[ERR]\033[0m %s\n" "$1"; exit 1; }

[ "$(id -u)" = "0" ] || die "请用 root 执行（sudo bash $0）"
[ -d "$SRC" ] || die "找不到配置源目录：$SRC"

# ---------- 0. 备份 ----------
log "备份现有配置"
BAK="/etc/nginx/backup-$TS"
mkdir -p "$BAK"
[ -f /etc/nginx/sites-available/rural-travel ] && cp /etc/nginx/sites-available/rural-travel "$BAK/" || true
[ -f /etc/nginx/conf.d/00-rural-limits.conf ] && cp /etc/nginx/conf.d/00-rural-limits.conf "$BAK/" || true
[ -f /etc/nginx/snippets/rural-travel-common.conf ] && cp /etc/nginx/snippets/rural-travel-common.conf "$BAK/" || true
[ -f "$ENV_FILE" ] && cp "$ENV_FILE" "$BAK/env.prod.$TS" || true
ok "备份目录 $BAK"

# ---------- 1. 安装 Nginx 配置 ----------
log "安装限流区定义 → /etc/nginx/conf.d/00-rural-limits.conf"
install -m 644 "$SRC/00-rural-limits.conf" /etc/nginx/conf.d/00-rural-limits.conf

log "安装通用规则 → /etc/nginx/snippets/rural-travel-common.conf"
mkdir -p /etc/nginx/snippets
install -m 644 "$SRC/rural-travel-common.conf" /etc/nginx/snippets/rural-travel-common.conf

log "安装站点配置 → /etc/nginx/sites-available/rural-travel"
cp "$SRC/rural-travel-domain.conf" /etc/nginx/sites-available/rural-travel
sed -i "s/xiangyezhiyou\.cn/${DOMAIN}/g" /etc/nginx/sites-available/rural-travel
ln -sfn /etc/nginx/sites-available/rural-travel /etc/nginx/sites-enabled/rural-travel

log "校验 Nginx 语法"
if nginx -t 2>/tmp/nginx-test.err; then
  ok "语法通过"
else
  cat /tmp/nginx-test.err
  log "回滚 Nginx 配置"
  cp "$BAK/rural-travel" /etc/nginx/sites-available/rural-travel 2>/dev/null || true
  die "配置有误，已回滚，Nginx 未重载"
fi

log "重载 Nginx"
systemctl reload nginx
sleep 1
ok "已重载"

# ---------- 2. 固定 SITE_BASE_URL 为回环 ----------
log "设置 SITE_BASE_URL=http://127.0.0.1"
if [ -f "$ENV_FILE" ]; then
  CUR=$(grep -m1 '^SITE_BASE_URL=' "$ENV_FILE" | cut -d= -f2- | tr -d '"' || true)
  if [ "$CUR" = "http://127.0.0.1" ]; then
    ok "已是回环地址，无需修改"
  else
    if grep -q '^SITE_BASE_URL=' "$ENV_FILE"; then
      sed -i.bak-site "s|^SITE_BASE_URL=.*|SITE_BASE_URL=http://127.0.0.1|" "$ENV_FILE"
    else
      echo 'SITE_BASE_URL=http://127.0.0.1' >> "$ENV_FILE"
    fi
    chmod 600 "$ENV_FILE"
    ok "已改为 http://127.0.0.1（原值：${CUR:-空}）"
  fi
else
  die "缺少 $ENV_FILE"
fi

# ---------- 3. 重启后端并等待健康 ----------
log "重启后端服务"
systemctl restart rural-travel
PORT=$(grep -m1 '^BACKEND_PORT=' "$ENV_FILE" | cut -d= -f2- | tr -d '"'); PORT="${PORT:-8091}"
for i in $(seq 1 45); do
  if curl -sf -o /dev/null --max-time 3 "http://127.0.0.1:${PORT}/api/categories"; then
    ok "后端就绪（约 $((i*2))s）"; break
  fi
  sleep 2
  [ "$i" = "45" ] && die "后端 90s 内未就绪，查看：journalctl -u rural-travel -n 50"
done

# ---------- 4. 回归验证 ----------
log "回归验证"
for path in / /bigscreen /api/categories /images/cover-7.jpg /scenic/7; do
  for port in 80 8080; do
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 8 -H "Host: $DOMAIN" "http://127.0.0.1:${port}${path}" || echo 000)
    printf "  :%-4s %-22s %s\n" "$port" "$path" "$code"
  done
done

IPCODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 8 "http://127.0.0.1/" || echo 000)
[ "$IPCODE" = "200" ] && ok "IP 直连首页仍为 200（未破坏原有访问）" || die "IP 直连首页异常：$IPCODE"

log "识景抓图基准验证"
IMGC=$(curl -s -o /dev/null -w "%{http_code}" --max-time 8 "http://127.0.0.1/images/cover-7.jpg" || echo 000)
[ "$IMGC" = "200" ] && ok "http://127.0.0.1/images/cover-7.jpg → $IMGC" || die "回环抓图失败：$IMGC"

printf "\n\033[32m准备完成。\033[0m\n"
printf "  下一步：到阿里云「云解析 DNS」添加 A 记录（@ 与 www → 本机公网 IP）\n"
printf "  解析生效后执行自检：bash %s/scripts/check-domain.sh %s\n" "$(dirname "${BASH_SOURCE[0]}")/.." "$DOMAIN"

#!/usr/bin/env bash
# ============================================================
# 乡野智游 —— 备案通过后一键上 HTTPS
#
# 前置条件：
#   - ICP 备案已通过（域名 80 端口不再被拦截）
#   - 域名 A 记录已指向本机（bash check-domain.sh 全绿）
#   - certbot 已安装（prepare-domain.sh 之外单独执行 apt install certbot python3-certbot-nginx）
#
# 动作：
#   1. 删除 8080 自测 server 块（标记区间）
#   2. certbot 签发 Let's Encrypt 证书并自动改造成 80→443 跳转
#   3. 验证证书、跳转、接口、静态资源
#   4. 演练一次自动续期
#
# 用法：sudo bash enable-https.sh [域名] [邮箱]
# ============================================================
set -euo pipefail

DOMAIN="${1:-xiangyezhiyou.cn}"
EMAIL="${2:-}"
SITE_CONF="/etc/nginx/sites-available/rural-travel"
TS=$(date +%Y%m%d-%H%M%S)

log() { printf "\033[36m==>\033[0m %s\n" "$1"; }
ok()  { printf "  \033[32m[OK]\033[0m %s\n" "$1"; }
die() { printf "  \033[31m[ERR]\033[0m %s\n" "$1"; exit 1; }

[ "$(id -u)" = "0" ] || die "请用 root 执行"
[ -n "$EMAIL" ] || die "缺少邮箱参数：bash $0 $DOMAIN you@example.com"

log "前置检查"
RESOLVED=$(getent ahostsv4 "$DOMAIN" | awk '{print $1; exit}' || true)
[ -n "$RESOLVED" ] || die "$DOMAIN 无法解析，先加 A 记录"
ok "$DOMAIN -> $RESOLVED"
code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 12 "http://$DOMAIN/" || echo 000)
[ "$code" = "200" ] || die "http://$DOMAIN 返回 $code（备案未通过时 80 端口会被拦截，请等备案生效）"
ok "80 端口已放行（$code）"

command -v certbot >/dev/null 2>&1 || die "certbot 未安装：apt install -y certbot python3-certbot-nginx"

# ---------- 1. 删除 8080 自测段 ----------
log "移除备案前自测端口 8080"
BAK="/etc/nginx/backup-https-$TS"
mkdir -p "$BAK" && cp "$SITE_CONF" "$BAK/rural-travel"
if grep -q "pre-filing-8080" "$SITE_CONF"; then
  sed -i '/# >>> pre-filing-8080 >>>/,/# <<< pre-filing-8080 <<</d' "$SITE_CONF"
  ok "已删除（备份：$BAK/rural-travel）"
else
  ok "无 8080 段，跳过"
fi

nginx -t || { cp "$BAK/rural-travel" "$SITE_CONF"; die "语法检查失败，已回滚"; }
systemctl reload nginx
ss -lnt | grep -q ":8080 " && die "8080 仍在监听" || ok "8080 已关闭"

# ---------- 2. 签发证书 ----------
log "申请 Let's Encrypt 证书（含 www 子域）"
certbot --nginx \
  -d "$DOMAIN" -d "www.$DOMAIN" \
  --redirect --agree-tos --no-eff-email \
  -m "$EMAIL" --non-interactive \
  || die "签发失败，查看 /var/log/letsencrypt/letsencrypt.log"

# ---------- 3. 验证 ----------
log "验证 HTTPS"
if [ -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
  EXP=$(openssl x509 -enddate -noout -in "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" | cut -d= -f2)
  ok "证书到期：$EXP"
else
  die "证书文件不存在"
fi

for path in / /bigscreen /api/categories /images/cover-7.jpg; do
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 12 "https://$DOMAIN$path" || echo 000)
  printf "  https %-22s %s\n" "$path" "$code"
done

RCODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 12 "http://$DOMAIN/" || echo 000)
[ "$RCODE" = "301" ] || [ "$RCODE" = "308" ] && ok "HTTP 自动跳转 HTTPS（$RCODE）" || printf "  \033[33m[--]\033[0m HTTP 返回 $RCODE（未跳转，可手动检查）\n"

log "演练自动续期"
certbot renew --dry-run >/dev/null 2>&1 && ok "续期演练通过（systemd timer 已随包启用）" || printf "  \033[33m[--]\033[0m 续期演练未通过，检查 certbot.timer\n"

printf "\n\033[32mHTTPS 已就绪。\033[0m\n"
printf "  站点地址：https://%s\n" "$DOMAIN"
printf "  注意：SITE_BASE_URL 保持 http://127.0.0.1 即可，无需改为 https（后端抓图走回环）\n"

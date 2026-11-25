#!/usr/bin/env bash
# ============================================================
# 乡野智游 —— 域名接入状态检查（只读，不改任何配置）
# 用法：bash check-domain.sh [域名]
# 默认域名：xiangyezhiyou.cn
# ============================================================
set -uo pipefail

DOMAIN="${1:-xiangyezhiyou.cn}"

# 公网 IP 不硬编码：
#   用法 1：bash check-domain.sh <域名> <公网IP>   显式指定
#   用法 2：bash check-domain.sh <域名>            自动探测
detect_public_ip() {
  local ip="" ep
  for ep in "https://ip.3322.net" "https://myip.ipip.net/s" "https://api.ipify.org"; do
    ip=$(curl -s --max-time 6 "$ep" 2>/dev/null | tr -dc '0-9.' | head -c 32)
    [ -n "$ip" ] && { printf '%s' "$ip"; return; }
  done
}
PUBLIC_IP="${2:-}"
OK=0; BAD=0

pass() { printf "  \033[32m[OK]\033[0m   %s\n" "$1"; OK=$((OK+1)); }
fail() { printf "  \033[31m[ERR]\033[0m  %s\n" "$1"; BAD=$((BAD+1)); }
warn() { printf "  \033[33m[--]\033[0m   %s\n" "$1"; }
head_() { printf "\n\033[1m%s\033[0m\n" "$1"; }

head_ "1. 本机对外 IP"
MYIP=$(detect_public_ip); MYIP="${MYIP:-unknown}"
[ -z "$PUBLIC_IP" ] && PUBLIC_IP="$MYIP"
if [ "$MYIP" = "unknown" ]; then
  warn "无法探测公网出口 IP（不影响后续检查）"
elif [ "$MYIP" = "$PUBLIC_IP" ]; then
  pass "公网出口 $MYIP 与预期一致"
else
  warn "公网出口为 $MYIP（预期 $PUBLIC_IP，若走 NAT 可忽略）"
fi
TARGET="$PUBLIC_IP"
IP_DISPLAY="$PUBLIC_IP"
[ "$TARGET" = "unknown" ] && { TARGET=""; IP_DISPLAY="<本机公网IP>"; }

head_ "2. 域名解析"
RESOLVED=""
for i in 1 2 3; do
  RESOLVED=$(getent ahostsv4 "$DOMAIN" 2>/dev/null | awk '{print $1; exit}')
  [ -n "$RESOLVED" ] && break
  sleep 2
done
if [ -z "$RESOLVED" ]; then
  fail "$DOMAIN 无 A 记录（解析未添加或未生效）"
  warn "请在域名的权威 DNS 控制台添加：A @ -> $IP_DISPLAY 与 A www -> $IP_DISPLAY"
elif [ -z "$TARGET" ]; then
  warn "$DOMAIN -> $RESOLVED（未能确定目标 IP，仅显示解析结果）"
elif [ "$RESOLVED" = "$TARGET" ]; then
  pass "$DOMAIN -> $RESOLVED"
else
  fail "$DOMAIN -> $RESOLVED（与服务器 $TARGET 不一致）"
fi

WWWRES=$(getent ahostsv4 "www.$DOMAIN" 2>/dev/null | awk '{print $1; exit}')
if [ -n "$TARGET" ] && [ "$WWWRES" = "$TARGET" ]; then
  pass "www.$DOMAIN -> $WWWRES"
else
  warn "www.$DOMAIN 解析为「${WWWRES:-无}」"
fi

head_ "3. Nginx 是否已监听域名"
if nginx -T 2>/dev/null | grep -q "server_name.*${DOMAIN}"; then
  pass "站点配置已包含 $DOMAIN"
else
  fail "站点配置未包含 $DOMAIN（仍是纯 IP 版）"
fi
for p in 80 8080; do
  if ss -lnt 2>/dev/null | grep -q ":${p} "; then
    pass "端口 $p 已监听"
  else
    [ "$p" = "8080" ] && warn "端口 8080 未监听（仅影响备案前自测）" || fail "端口 $p 未监听"
  fi
done

head_ "4. 本机按 Host 头访问（不依赖 DNS，验证 nginx 路由）"
for scheme_port in "80" "8080"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 8 \
         -H "Host: $DOMAIN" "http://127.0.0.1:${scheme_port}/" 2>/dev/null)
  if [ "$code" = "200" ]; then
    pass "Host:$DOMAIN :$scheme_port 首页 $code"
  else
    fail "Host:$DOMAIN :$scheme_port 首页 $code"
  fi
done

head_ "5. 公网实访（注意：本机访问自己的公网 IP 会走 NAT 回环，绕过云厂商拦截，不能代表外网结果）"
code80=$(curl -s -o /dev/null -w "%{http_code}" --max-time 12 "http://$DOMAIN/" 2>/dev/null || true); code80=${code80:-000}
case "$code80" in
  200) warn "本机访问 http://$DOMAIN 返回 200（NAT 回环，不能证明外网可达；请用手机流量或外部网络复核）" ;;
  000) fail "本机无法连接 http://$DOMAIN（解析未生效或端口被拦）" ;;
  4*|5*) fail "本机访问返回 $code80（多为备案未通过时的拦截页）" ;;
  *)   warn "本机访问返回 $code80" ;;
esac

code8080=$(curl -s -o /dev/null -w "%{http_code}" --max-time 12 "http://$DOMAIN:8080/" 2>/dev/null || true); code8080=${code8080:-000}
if [ "$code8080" = "200" ]; then
  pass "http://$DOMAIN:8080 返回 200 —— 解析与 Nginx 均已就绪，只等备案"
elif [ "$code8080" = "000" ]; then
  warn "http://$DOMAIN:8080 不通（检查阿里云防火墙是否放行 8080）"
else
  warn "http://$DOMAIN:8080 返回 $code8080"
fi

head_ "6. HTTPS 证书"
if [ -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
  EXP=$(openssl x509 -enddate -noout -in "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" 2>/dev/null | cut -d= -f2)
  pass "证书已签发，到期时间 $EXP"
else
  warn "尚未签发证书（备案通过后执行 enable-https.sh）"
fi

head_ "7. 后端站点地址（识景抓图基准）"
SITE=$(grep -m1 '^SITE_BASE_URL=' /opt/rural-travel/.env 2>/dev/null | cut -d= -f2- | tr -d '"')
if [ -n "$SITE" ]; then
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 8 "$SITE/images/cover-7.jpg" 2>/dev/null || true); code=${code:-000}
  if [ "$code" = "200" ]; then
    pass "SITE_BASE_URL=$SITE 可抓图（$code）"
  else
    fail "SITE_BASE_URL=$SITE 抓图失败（$code）—— 识景会静默失效"
  fi
else
  fail "未读到 SITE_BASE_URL"
fi

printf "\n\033[1m汇总：%d 项通过，%d 项异常\033[0m\n" "$OK" "$BAD"
[ "$BAD" -eq 0 ] && exit 0 || exit 1

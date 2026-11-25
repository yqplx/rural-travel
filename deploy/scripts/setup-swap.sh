#!/usr/bin/env bash
# ============================================================================
# 为小内存机器添加 swap（默认 2G），幂等，可重复执行
# 用法：sudo bash deploy/scripts/setup-swap.sh [大小GB，默认2]
# 适用：2 核 2G 的云服务器，防止 JVM + MySQL 峰值把进程 OOM 掉
# ============================================================================
set -euo pipefail

SIZE_GB="${1:-2}"
SWAPFILE=/swapfile

[ "$(id -u)" -eq 0 ] || { echo "请用 sudo 运行"; exit 1; }

if swapon --show | grep -q "^${SWAPFILE}"; then
  echo "swap 已启用，无需重复操作："
  free -h | grep -i swap
  exit 0
fi

if [ -f "$SWAPFILE" ]; then
  echo "已存在 $SWAPFILE，直接启用"
else
  echo "创建 ${SIZE_GB}G swap 文件..."
  if ! fallocate -l "${SIZE_GB}G" "$SWAPFILE" 2>/dev/null; then
    echo "  fallocate 不支持该文件系统，改用 dd（会慢一些）"
    dd if=/dev/zero of="$SWAPFILE" bs=1M count=$((SIZE_GB * 1024)) status=progress
  fi
fi

chmod 600 "$SWAPFILE"
mkswap "$SWAPFILE" >/dev/null
swapon "$SWAPFILE"

# 开机自动挂载（幂等写入）
grep -qE "^${SWAPFILE}[[:space:]]" /etc/fstab || echo "$SWAPFILE none swap sw 0 0" >> /etc/fstab

# 压低换出倾向：优先用物理内存，内存不够时才换出，避免 JVM 无故变慢
sysctl -w vm.swappiness=10 >/dev/null
grep -q "^vm.swappiness=10" /etc/sysctl.conf || echo "vm.swappiness=10" >> /etc/sysctl.conf

echo
echo "完成，当前内存状态："
free -h
echo
swapon --show

# 乡野智游 · 部署运维手册

面向要把它放到公网跑的人。本地开发/分发版的启动方式见项目根目录 `README.md`，本文只讲**上线**。

> 结论先行：这不是静态站。前端可以纯静态，但 `/api` 必须同域反代到 Spring Boot，后端再连 MySQL，所以**必须有一台常开的 Linux 机器**。

---

## 0. 前置条件

| 项 | 要求 | 说明 |
|---|---|---|
| 服务器 | 2 核 4G 起，Ubuntu 22.04 / Debian 12 / CentOS Stream 9 | 1G 堆 + MySQL + Nginx，2G 内存会在构建时 OOM |
| 域名 | 国内服务器**必须备案**（1–2 周） | 不想等就用香港/境外机，免备案但延迟高 |
| JDK | **17**（21 亦可） | `java -version` 必须是 17/21，systemd 读 `/usr/bin/java` |
| MySQL | 8.0 | 字符集必须 utf8mb4 |
| Nginx | 1.20+ | 用于静态托管 + 反代 + TLS |
| 磁盘 | ≥ 20G | 前端产物 103M + 后端 35M + 数据库 + 备份 |

⚠️ 仓库里那份 `runtime/` 是 **Windows 便携版** JDK/MySQL/Node，Linux 上完全不能用，别传上去。

---

## 1. 目录约定

```
/opt/rural-travel/
├─ .env                      # 环境变量，权限 600，属主 rural（唯一放明文口令的地方）
├─ config/
│  └─ application-prod.yml   # 外置生产配置（不进 jar，改配置不用重新打包）
├─ releases/
│  ├─ 20261118-1600/
│  │  ├─ app.jar
│  │  └─ www/                # 前端 dist 内容
│  └─ 20261119-0930/
├─ app.jar -> releases/最新/app.jar     # systemd 用这个软链
├─ www     -> releases/最新/www         # Nginx root 指这里
├─ backups/                  # 数据库备份
└─ DEPLOY.md
```

发版 = 新建一个 `releases/` 目录 + 切软链，**旧版本原样保留**，所以回滚只是把软链指回去。

---

## 2. 首次部署

### 2.1 装环境

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk mysql-server nginx rsync curl
java -version    # 确认 17
```

### 2.2 建库与最小权限账号

```bash
sudo mysql -e "CREATE DATABASE IF NOT EXISTS rural_travel DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'rural'@'localhost' IDENTIFIED BY '换成你的强口令';"
sudo mysql -e "GRANT SELECT,INSERT,UPDATE,DELETE ON rural_travel.* TO 'rural'@'localhost';"
sudo mysql -e "FLUSH PRIVILEGES;"
```

> 生产环境 `spring.sql.init.mode=never`，建表由你手工完成，所以**不需要** DDL 权限。

### 2.3 迁移数据（二选一）

**A. 带现有数据上线**（推荐，内容和本地一致）

本地 Windows 上导出（注意是 3307 端口，别写成 3306）：

```bat
cd /d Z:\big\rural-travel
runtime\mysql\bin\mysqldump.exe -u root -proot123456 -P 3307 ^
  --single-transaction --routines --triggers --default-character-set=utf8mb4 ^
  --databases rural_travel > rural_travel.sql
```

上传并导入：

```bash
scp rural_travel.sql user@server:/tmp/
sudo mysql < /tmp/rural_travel.sql
sudo mysql -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='rural_travel';"   # 应为 24
```

**B. 空库重建**：什么都不做。首次启动时 `DataInitializer` 会自动灌入种子数据（各 seed 方法都有 `count==0` 守卫，不会重复插入）。

### 2.4 构建产物

后端（**必须在有 Maven 的机器上做**，仓库不含 jar）：

```bash
cd app/backend
mvn -DskipTests clean package
# 产物：target/rural-travel-backend.jar
```

前端：

```bash
cd app/frontend
npm ci          # 严格按 package-lock.json 安装
npm run build   # 产物：dist/（约 103M，含 images 87M、cesium 13M）
```

### 2.5 配置

```bash
sudo mkdir -p /opt/rural-travel/{config,backups}
sudo cp deploy/config/application-prod.yml /opt/rural-travel/config/
sudo cp deploy/env.prod.example /opt/rural-travel/.env
sudo nano /opt/rural-travel/.env          # 改 DB_PASS / JWT_SECRET / SITE_BASE_URL
sudo chmod 600 /opt/rural-travel/.env
sudo chown -R rural:rural /opt/rural-travel 2>/dev/null || true
```

`JWT_SECRET` 生成：`openssl rand -base64 48 | tr -d '\n'`

> **AI_CRYPTO_SECRET 的取舍**：要从现有库迁移用户已存的 AI Key，就必须保留旧值 `rural-travel-ai-crypto-2026`；换成新值则所有已存 Key 解不开，用户得重填。

### 2.6 Nginx 与证书

```bash
sudo cp deploy/nginx/rural-travel.conf /etc/nginx/conf.d/
sudo sed -i 's/example.com/你的域名/g' /etc/nginx/conf.d/rural-travel.conf
sudo nginx -t && sudo systemctl reload nginx

# 证书（域名已解析到本机后）
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d 你的域名
```

没有域名、只想用 IP 跑通（临时答辩）：用配置文件末尾注释里的场景 B 段落。

### 2.7 systemd 服务

```bash
sudo cp deploy/systemd/rural-travel.service /etc/systemd/system/
sudo systemctl daemon-reload
```

### 2.8 部署

```bash
sudo bash deploy/scripts/deploy.sh \
  --jar app/backend/target/rural-travel-backend.jar \
  --www app/frontend/dist
```

脚本会自动：建 `rural` 用户 → 建发布目录 → 拷产物 → 切软链 → 重启 → 轮询 `/api/categories` 直到 200 → 失败自动回退并打印日志。

### 2.9 上线前自检

```bash
sudo bash deploy/scripts/preflight.sh
```

全 PASS 再放流量。

---

## 3. 日常发版 / 回滚 / 备份

```bash
# 发版（同上，每次生成新 release，自动保留 5 份）
sudo bash deploy/scripts/deploy.sh --jar <新jar> --www <新dist>

# 回滚到上一个版本
sudo bash deploy/scripts/rollback.sh

# 看有哪些版本
sudo bash deploy/scripts/rollback.sh --list

# 备份数据库
sudo bash deploy/scripts/backup-db.sh 7
```

定时任务：

```bash
sudo crontab -e
# 每天 03:30 备份
30 3 * * * /usr/bin/bash /opt/rural-travel/deploy/scripts/backup-db.sh 7 >> /var/log/rural-travel/backup.log 2>&1
# 每 5 分钟巡检
*/5 * * * * /usr/bin/bash /opt/rural-travel/deploy/scripts/healthcheck.sh >> /var/log/rural-travel/health.log 2>&1
# 每天清理 30 天前的 sys_log（该表无内置清理任务，长期不清理会一直涨）
0 4 * * * mysql -u rural -p'口令' rural_travel -e "DELETE FROM sys_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);" >> /var/log/rural-travel/purge.log 2>&1
```

> `sys_log` 的时间字段是 `created_at`（有 `idx_time` 索引，按时间删不会全表扫）。

---

## 4. 日志与监控

| 内容 | 位置 |
|---|---|
| 应用日志 | `/var/log/rural-travel/app.log`（50MB 滚动，保留 14 天，总上限 1GB） |
| 访问日志 | `/var/log/rural-travel/access.log`（Tomcat 层，含耗时 %D） |
| 进程输出 | `journalctl -u rural-travel -f` |
| 巡检 | `deploy/scripts/healthcheck.sh`，异常时向 `WEBHOOK_URL` 推企业微信/钉钉消息 |

告警接入（企业微信机器人）：

```bash
export WEBHOOK_URL='https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx'
```

---

## 5. 安全清单

- [x] 后端只监听 `127.0.0.1:8091`，公网流量只能经 Nginx
- [x] MySQL 只监听 127.0.0.1，应用用最小权限账号（仅 DML）
- [x] 口令/密钥只在 `/opt/rural-travel/.env`，权限 600，不进仓库
- [x] systemd 加固：`ProtectSystem=strict`、`NoNewPrivileges`、`PrivateTmp`
- [ ] **改掉默认管理员口令**（源码 seed 里带一组默认管理员口令，公网一开等于后台裸奔）
- [ ] `JWT_SECRET` 换成强随机值（默认值是源码里公开的字符串，等于可伪造任意用户 token）
- [ ] `AI_API_KEY` 保持留空（`/api/ai/chat`、`/api/ai/recognize` 免登录，配了公共 Key 会被刷）
- [ ] 防火墙只放 80/443：`ufw allow 80,443/tcp && ufw enable`

---

## 6. 排错

| 现象 | 原因 | 处理 |
|---|---|---|
| 后端起不来，日志报 `Could not resolve placeholder 'SPRING_DATASOURCE_URL'` | `.env` 没加载或变量名拼错 | 检查 `EnvironmentFile` 与 `deploy.sh` 是否读同一份 |
| 启动报 `Table 'rural_travel.user' doesn't exist` | `sql.init.mode=never` 且没导入数据 | 执行 2.3（导入或建表） |
| 页面 200 但接口 502 | 后端没起 / 端口不符 | `systemctl status rural-travel`、`ss -ltn \| grep 8091` |
| 刷新 `/bigscreen` 404 | Nginx 少了 `try_files` | 确认配置里有 `try_files $uri $uri/ /index.html` |
| 识景一直匹配不到 | `SITE_BASE_URL` 不对，抓不到图 | 改成公网前端地址，注意不带结尾斜杠 |
| 上传报 413 | Nginx `client_max_body_size` 小于文件 | 已设 12m，与后端 10MB 对齐 |
| 大屏 3D 地图空白 | `/cesium/` 静态资源没被服务 | `curl -I https://域名/cesium/Widgets/widgets.css` 应 200 |
| 时间差 8 小时 | 服务器时区不是 Asia/Shanghai | `timedatectl set-timezone Asia/Shanghai` |

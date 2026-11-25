# 乡野智游 · 一键配置与启动说明（组员版）

面向新组员：解压后按本文操作，无需在电脑上安装 JDK / MySQL / Node / Maven，全部运行时已随包携带。

> **线上访问**：http://xiangyezhiyou.cn ｜ 备用 IP 直连：http://101.200.168.106
> （域名备案通过前 80 端口可能被拦截，此时改用 IP 地址，功能完全一致）

---

## 0. 先决条件（最重要，违反必失败）

**必须把本包解压到「纯英文、无空格」路径**，例如：

```
D:\rural-travel\
```

禁止：`C:\Users\中文名\...`、`...\桌面\...`、带空格的路径、`C:\Program Files\...`。

原因：MySQL 与 esbuild 的底层程序在中文/空格路径下会直接崩溃（`CreateProcess Failed 5` / `winmm.dll not found`），这是实测坑，不是配置问题。

系统要求：Windows 10/11 64 位；浏览器 Chrome/Edge。

---

## 1. 目录结构

```
rural-travel/
├─ start.bat          ← 双击：一键启动（推荐）
├─ stop.bat           ← 双击：一键停止
├─ status.bat         ← 双击：检查三服务状态
├─ config.bat         ← 端口/口令配置（一般不用改）
├─ README.md          ← 本文件
├─ runtime/           ← 运行环境（JDK17 / MySQL shim / Node / esbuild）
├─ app/backend/       ← 后端 jar + 源码
├─ app/frontend/      ← 前端源码 + node_modules
├─ data/mysql-data/   ← 真实数据库（勿删）
└─ docs/              ← 文档、截图、设计稿、专利交底书
```

---

## 2. 启动（日常只需这一步）

1. 双击 `start.bat`
2. 会弹出 3 个黑窗口：MySQL / 后端 / 前端；脚本会**逐个等服务真正就绪**：
   ```
   [OK] MySQL 3307 is up.
   [OK] Backend 8091 is up.
   [OK] Frontend 5174 is up.
   ALL READY.
   ```
3. 服务就绪后，打开浏览器访问已部署的线上站点：

| 用途 | 地址（域名） | 备用（IP 直连） |
|---|---|---|
| 游客端 | http://xiangyezhiyou.cn | http://101.200.168.106 |
| 数据大屏 | http://xiangyezhiyou.cn/bigscreen | http://101.200.168.106/bigscreen |
| 后端 API | http://xiangyezhiyou.cn/api | http://101.200.168.106/api |

> 域名 ICP 备案通过前，80 端口访问可能被云厂商拦截（返回提示页）。遇到这种情况用右侧 **IP 直连**地址，功能完全一致。备案完成后域名正式启用，并自动跳转 HTTPS。

默认账号（本地分发包）：管理员 `admin / admin123`，普通用户 `test / 123456`。

> ⚠️ 以上是**本地分发包**首次建库时的默认口令。线上环境的管理员口令已单独修改，与此不同，不在此文档公开。

**看到 `ALL READY` 之前不要打开浏览器**，否则会看到 `ECONNREFUSED`（后端还没起来）——这是时序问题，不是故障。

---

## 3. 停止

双击 `stop.bat`（按端口精确结束进程），或直接关掉那 3 个黑窗口。

检查状态：双击 `status.bat`，三个端口都应显示 `[运行中]`。

---

## 4. 配置（仅端口冲突时需要）

双击 `config.bat` 查看；如需改端口，编辑其中的 `MYSQL_PORT` / `BACKEND_PORT` / `FRONTEND_PORT`，保存后重新运行 `start.bat`。脚本会按新端口生成 `runtime/mysql/my.ini` 和前端代理配置。

数据库口令默认 `root / root123456`，与 `data/mysql-data` 中的数据一致，不要改；改了会导致后端连不上库。

AI 功能：各用户在「个人中心 → AI 设置」填自己的 Key；未填则降级本地引擎，不影响其他功能。

---

## 5. 常见问题（按现象查）

| 现象 | 原因 | 处理 |
|---|---|---|
| `CreateProcess Failed (5)` / `winmm.dll not found` | 解压路径含中文或空格 | 移到 `D:\rural-travel\` 这类纯英文路径 |
| 页面 `ECONNREFUSED 127.0.0.1:8091` | 浏览器开太早 / 后端没起 | 等 `ALL READY`；或看后端窗口是否有 `Started RuralTravelApplication` |
| 提示端口已被占用 | 本机已有其他程序占 3307/8091/5174 | 改 `config.bat` 端口，或先双击 `stop.bat` |
| 后端 `Communications link failure` | MySQL 没起来 | 先确认 MySQL 窗口在、3307 LISTENING |
| 识景哈希预热 `0/100` | 预热时前端还没起 | 正常，刷新页面不影响主要功能 |
| 双击 bat 一闪而过 | bat 编码被改坏 | 本包 bat 为 GBK+CRLF，勿用编辑器另存为 UTF-8 |
| `Failed to resolve entry for package "@antv/g6"` | `node_modules` 被换成了残缺副本，或旧 `.vite` 缓存路径失配 | 本包已按 `package-lock.json` 完整重装依赖，`start-frontend.bat` 启动前会自动清理 `.vite`；不要从别处复制 `node_modules` 覆盖 |

排查命令（在包目录打开 cmd）：

```bat
netstat -ano | findstr ":3307 " | findstr LISTENING
netstat -ano | findstr ":8091 " | findstr LISTENING
netstat -ano | findstr ":5174 " | findstr LISTENING
```

---

## 6. 数据说明

- `data/mysql-data/` 是项目真实数据（24 张表、景区与图谱数据）。**不要删除、不要与另一份副本的数据混用**，否则会出现"表不存在/数据没变"。
- 修改代码后：后端改 Java 需重新打包 jar 覆盖 `app/backend/rural-travel-backend.jar`；前端改 Vue 源码热更新直接生效。
- 本包不包含 Maven（无需重新构建），也不要求组员安装任何开发环境。
- `app/frontend/node_modules` 已按 `package-lock.json` 完整重装（2026-11-17），不要用源项目里的那份覆盖；源项目的副本缺少 `@antv/g6`、`lodash` 等包的部分文件，删掉 `.vite` 后会触发 Vite 依赖扫描失败。
- `start-frontend.bat` 每次启动前会自动删除 `node_modules\.vite`，避免换目录后旧缓存导致 `@antv/g6` 报错。

---

## 7. 技术栈（答辩用）

Spring Boot 3.2.5（内嵌 Tomcat 10）/ Vue 3 + Vite 5 / MySQL 8.0.28（便携版）/ JDK 17 / MyBatis-Plus / JWT / ECharts + G6 + Cesium。

端口：本地分发包用 MySQL 3307、后端 8091、前端 5174，三服务全部为本机进程，跑通不需要独立服务器。

线上环境另有一台 2 核 2 GiB 云服务器（Ubuntu 24.04）：MySQL 3306、后端 8091 只监听 127.0.0.1，对外仅由 Nginx 暴露 80/443 并反代 `/api`，systemd 托管、每日自动备份。部署细节见 `deploy/README.md`。

---

版本：2026-11-17 分发版（依赖完整版）。打包源：OneDrive 项目本体 + `D:\rural-launch` 已验证启动器；前端 `node_modules` 已按 lock 文件重装修复。

---

## 8. 打包前实测记录（2026-11-17，在 `D:\rural-travel-dist\rural-travel` 本机跑通）

| 检查项 | 结果 |
|---|---|
| MySQL 3307 | LISTENING（需预建 `<主机名>.err`，已固化进 `start-mysql.bat`） |
| 后端 8091 | LISTENING，`Started RuralTravelApplication in 4.574s`，无 ERROR |
| 前端 5174 | LISTENING |
| `http://localhost:5174/` | **HTTP 200** |
| `http://localhost:5174/bigscreen` | **HTTP 200** |
| `http://localhost:8091/api/categories` | **HTTP 200** |
| 前端代理 `5174/api/categories` | **HTTP 200**（代理链路一致） |

结论：从分发目录、用包内 JDK/MySQL/Node 启动，全部接口通过；组员只需解压到纯英文路径后双击 `start.bat`。

---

## 9. 线上部署实测记录（2026-11-25）

| 项 | 内容 |
|---|---|
| 服务器 | 2 vCPU / 2 GiB / 40 GiB，Ubuntu 24.04，华北2（北京） |
| 运行时 | OpenJDK 17.0.21 / MySQL 8.0.46 / Nginx 1.24 |
| 访问结构 | Nginx 托管前端 `dist` 静态资源，`/api` 反代到 `127.0.0.1:8091`，同域无跨域 |
| 数据库 | 24 张表；业务账号 `rural` 仅有 SELECT / INSERT / UPDATE / DELETE |
| 运维 | systemd 托管（`-Xmx512m` + 2G swap）、每日 03:30 自动备份、`deploy/` 内含回滚脚本 |

外网实测（地址见文首）：

| 检查项 | 结果 |
|---|---|
| `/` | HTTP 200 |
| `/bigscreen` | HTTP 200（history 路由回退生效） |
| `/api/categories` | HTTP 200，返回真实数据 |
| `/images/cover-7.jpg` | HTTP 200 |
| `/scenic/7` | HTTP 200 |
| 管理员登录 | 返回 JWT，含 ADMIN 角色 |
| 识景哈希预热 | 100/100 成功 |
| 后端日志 | 0 条 ERROR |

部署手册与脚本见 `deploy/`：`prepare-domain.sh`（域名配置）、`check-domain.sh`（状态自检）、`enable-https.sh`（HTTPS）、`deploy.sh` / `rollback.sh`（发布与回滚）、`backup-db.sh`（备份）。

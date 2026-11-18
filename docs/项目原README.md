# 乡野智游 · 轻量化AI乡村文旅智能引流与服务系统

基于 **Spring Boot 3 + Vue 3** 的乡村文旅智能推荐与服务系统，集个性化推荐、智能问答、拍照识景、语音导游、数据分析与 AIGC 内容生成于一体，全程轻量化、零外部强依赖（未配置大模型 Key 也能完整运行）。

在原推荐系统之上，已融合 **方案三：面向县域文旅的 AI 多模态数字内容生成与智能导览系统**（S1 知识图谱 → S6 反馈闭环），详见下文 [县域智能导览（方案三）](#县域智能导览方案三s1s6) 与 `docs/政策契合说明.md`、`docs/patent/` 专利交底书。

## 目录结构

```
D:\workbuddy_rural\
├─ backend\            # Spring Boot 3 后端 (Java 17 + MyBatis-Plus + JWT + BCrypt)
├─ frontend\           # Vue 3 + Vite + Element Plus 前端 SPA
│   └─ public\images\  # 404 张本地生成的景区图片(离线可用,不依赖任何外链图床)
├─ tools\              # 便携运行环境 (JDK17 / Maven / MySQL 8.0，均免安装)
│   ├─ jdk17\
│   ├─ maven\
│   ├─ mysql\
│   │   └─ my.ini      # MySQL 固化配置(datadir/端口/init-file)
│   ├─ mysql-data\     # MySQL 数据目录(首次启动自动初始化)
│   └─ mysql-init.sql  # 每次启动自动重置 root 密码为 root123456
├─ start-mysql.bat     # 一键启动数据库
├─ start-backend.bat   # 一键启动后端
└─ start-frontend.bat  # 一键启动前端
```

## 技术架构

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Vue Router + Axios + Element Plus（加载页/首页/列表/详情/注册/登录/个人中心/识景/管理端，清新田园 UI） |
| 后端 | Spring Boot 3 分层架构：controller / service / mapper / entity / dto / vo / config / security / recommend / exception / utils / ai |
| 安全 | JWT 无状态鉴权 + BCrypt 密码哈希 + Spring Security 角色控制（USER/ADMIN），Token 失效统一 401 JSON |
| 数据库 | MySQL 8.0，15 张表，首次启动自动建表 + 初始化全部种子数据与默认管理员 |
| AI 能力 | 大模型增强 + 本地轻量引擎双轨：知识库检索打分 / pHash 感知哈希 / 规则引擎 / 模板生成，未配置 Key 自动降级 |

## 核心功能

- **用户体系**：注册登录、BCrypt 哈希、JWT 鉴权、Token 失效处理、个人中心（资料/收藏/评分/浏览历史）
- **景区服务**：多条件筛选搜索（关键词/分类/标签/价格区间/排序）、详情、收藏、评分、浏览记录
- **个性化推荐**：内容特征匹配 + 用户行为相似性双路融合，结果落库缓存 1 小时；前端仅展示「为你推荐」「猜你喜欢」自然文案
- **AI 智能问答**：全站悬浮助手「小乡」，25 条预置知识库（门票/交通/住宿/美食/行程/文化/通用），游客免登录
- **拍照识景**：pHash 感知哈希本地匹配（毫秒级）+ 可选多模态大模型，游客免登录
- **语音导游**：详情页一键生成结构化讲解稿，浏览器 Web Speech API 分段播报，零成本 TTS
- **数据分析（管理端）**：30 天客流热力、24 小时分布、分类/标签热度、客源省份、价格偏好、周末占比 + 7 类规则引擎运营建议
- **AIGC 内容工作台（管理端）**：种草文案 / 游玩攻略 / 宣传标语 / 短视频脚本，模板引擎兜底
- **管理后台**：用户 / 景区 / 分类标签 / 评分数据 / 虚拟用户与虚拟评分样本 / AI 知识库 / 系统日志，全部 ADMIN 角色校验

## 县域智能导览（方案三，S1–S6）

在原推荐系统基础上新增的六大能力，对应专利《一种面向县域文旅的 AI 多模态数字内容生成与智能导览方法及系统》独立权利要求 S1–S6：

| 步骤 | 能力 | 游客/管理端页面 | 核心机制 |
|---|---|---|---|
| S1 | 县域文旅知识图谱 | `/guide/graph` · `/admin/guide-kg` | 26 实体（12景点+6非遗+8美食）/ 90 关系边，环状分区布局，节点大小=热度、橙环=拥挤度 |
| S2 | 多模态内容量产 | `/admin/guide-content` | 实体×模态×风格×口音组合批量生成，已存在自动跳过；实测 520 条内容、4/4 模态、260 条方言版 |
| S3 | 千人千面路线规划 | `/guide/plan` | 式(1) `s=0.40p+0.25h−0.20c+0.15g` 打分 + 式(2) 时间预算装配（τ=0.75），每站给出分数拆解与中文理由 |
| S4 | 游客画像 | 路线页内嵌表单 | 8 组输入（同行人/年龄/深度/时间/节奏/口音/兴趣/避堵），可保存并推理 |
| S5 | 实时触发导览 | `/guide/live` | 靠近自动触发；式(3) `d=⌊0.30k+0.25r+0.20q+1⌋` 实时算讲解深度（上限3）；Web Speech 方言播报；追问问答 |
| S6 | 反馈闭环 | `/guide/live` 反馈区 | 式(4) `h(t+1)=0.8h+0.2f` 指数滑动平均，评分同时回写内容质量分与实体热度，反哺式(1) |

- **政策契合**：三个「十五五」方向的落地证据见 `docs/政策契合说明.md`；管理端看板 `/admin/guide-metrics`。
- **种子数据**：演示县域「梅城古镇」由 `GuideDataInitializer` 自动播种，换县替换种子即可复制整套系统。
- **封面图**：`frontend/public/images/guide/`（14 张真实照片 + 12 张 AI 宣传画面，26/26 实体全覆盖）。
- **3D 素材**：非遗竹编灯笼模型在 `assets3d/`（glb/obj/预览图），不参与前端构建。
- **数据自愈**：种子数据（26 实体 / 90 关系 / 520 条内容 / 26 张封面）在后端启动时自动重建；
  路线、导览会话、反馈属于运行期活动，重建库后归零，跑一次即可复现：
  ```bat
  python tools/demo_walkthrough.py    :: 登录 → 规划 7 站路线 → 触发导览 → 打分回写
  ```

**新增 API**（均挂 `/api` 前缀）：

- 游客：`GET /api/guide/counties|graph|entities/{id}`、`GET|POST /api/guide/profile`、`POST /api/guide/profile/infer`、`POST /api/guide/route/plan`、`GET /api/guide/routes[/my]`、`POST /api/guide/trigger`、`POST /api/guide/ask`、`POST /api/guide/session/{id}/touch`、`POST /api/guide/feedback`、`GET /api/guide/feedback/my`
- 管理端（ADMIN）：`GET|POST|DELETE /api/admin/guide/entities[/relations]`、`POST /api/admin/guide/content/produce`、`POST /api/admin/guide/content/produce-all`、`GET|DELETE /api/admin/guide/contents`、`GET /api/admin/guide/metrics`

## 默认账号（首次启动自动创建）

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | 管理员（管理后台入口：右上角头像 → 管理后台） |
| `test` | `123456` | 普通用户 |
| `visitor01` ~ `visitor05` | `123456` | 演示游客（不同省份，用于客源分析） |

## 下次启动

### 方式一：一键启动（推荐）

双击 `启动全部.bat`，会自动依次打开 MySQL / 后端 / 前端三个最小化窗口，约 1 分钟后浏览器访问 **http://localhost:5173** 即可。

前端监听 `0.0.0.0`，所以 `http://localhost:5173`、`http://127.0.0.1:5173`、以及 `http://<本机局域网IP>:5173`（同一 WiFi 下的手机/其他电脑）都能打开。

### 方式二：三步走（手动逐个启动）

> 前提：三个服务各开一个窗口，按 1→2→3 顺序启动。

1. **启动数据库**：双击 `start-mysql.bat`
   - 首次运行会自动初始化数据目录（约 1-2 分钟），之后秒启
   - 窗口显示 `ready for connections` 即就绪（端口 3306，root 密码 `root123456`）
2. **启动后端**：双击 `start-backend.bat`
   - 首次启动自动建库建表、写入种子数据与默认管理员
   - 看到 `Started RuralTravelApplication` 即就绪（端口 8090）
3. **启动前端**：双击 `start-frontend.bat`
   - 浏览器访问 **http://localhost:5173**

关闭：直接关闭对应命令行窗口即可。

## 常见问题：页面提示「网络异常 / 无法连接服务器」

页面能打开但所有数据加载不出来，几乎都是**后端没在运行**。页面顶部会自动弹出红色提示条（每 20 秒自检一次，恢复后自动消失）。

自检：双击 `检查服务状态.bat`，正常应显示三项 `[运行中]`：

```
[运行中]  MySQL 数据库      端口 3306
[运行中]  后端 SpringBoot   端口 8090
[运行中]  前端 Vite         端口 5173
```

对应关系：

| 现象 | 原因 | 处理 |
|---|---|---|
| 3306 未运行 | 数据库窗口被关掉 | 双击 `start-mysql.bat` |
| 8090 未运行 | 后端窗口被关掉或未启动完 | 双击 `start-backend.bat`，等出现 `Started RuralTravelApplication` |
| 5173 未运行 | 前端未启动 | 双击 `start-frontend.bat` |
| 三项都在跑仍报错 | 前端代理未生效 | 重启前端窗口，浏览器 `Ctrl + F5` 硬刷新 |

> 注意：后端窗口关闭 = 服务停止。演示/开发期间请勿关闭那三个命令行窗口。

## 常见问题：MySQL 起不来（错误日志文件 Permission denied）

现象：`start-mysql.bat` 窗口一闪而过，3306 不监听；数据目录里的错误日志尾部写着

```
[ERROR] [MY-010187] Could not open file '...\tools\mysql-data\<主机名>.err' for error logging: Permission denied
[ERROR] [MY-010119] Aborting
```

原因：**错误日志文件 `<主机名>.err` 不存在且 mysqld 创建不出来**（项目放在 OneDrive 同步目录里时容易出现，同步引擎会占用目录），mysqld 拿不到日志就直接中止——与数据文件无关，数据不会丢。

一键修复：双击 **`start-mysql-repair.bat`**，它会先预创建错误日志文件，再用项目 `my.ini` 启动 mysqld（ASCII 编码，不会有中文乱码问题）。

手工修复（等价操作）：

```bat
cd /d 项目目录
echo. 2> tools\mysql-data\%COMPUTERNAME%.err
tools\mysql\bin\mysqld.exe --defaults-file=tools\mysql\my.ini
```

## 常见坑：同一个项目有两份副本

项目曾同时存在于 `D:\workbuddy_rural` 和当前的 OneDrive 目录。**两边各自带一套 MySQL 数据目录，数据不互通**——如果后端连到的是另一份副本的库，就会看到"表不存在 / 数据没变"。

统一做法：今后只在**本项目目录（OneDrive）**下用 `启动全部.bat` 启服务；启动前双击 `检查服务状态.bat` 确认三项 `[运行中]`。旧副本可以直接删掉或留作备份，但不要同时启两套 MySQL。

## 常见问题：后端启动报 `Access denied for user 'root'`

现象：后端窗口刷出 `java.sql.SQLException: Access denied for user 'root'@'localhost'`，然后进程退出、8090 不监听。

原因：MySQL 的 root 密码不是 `root123456`（多半是直接双击了 `tools\mysql\bin\mysqld.exe`，漏掉了 `--init-file` 参数）。

根治方案（已内置，无需手工操作）：

1. `tools\mysql\my.ini` 固化了 `datadir / port / init-file`，**无论用 bat 还是直接双击 mysqld.exe 都会自动加载**，密码不会再跑偏。
2. `start-mysql.bat` 每次启动都会按当前目录重写 `my.ini` 和 `mysql-init.sql`，项目整个目录换盘符 / 换电脑也不会失效。
3. `mysql-init.sql` 每次启动执行 `ALTER USER 'root'@'localhost' IDENTIFIED BY 'root123456'`，密码被改也会自动改回来。

验证方式：启动数据库后连一次 `tools\mysql\bin\mysql.exe -uroot -proot123456 -h127.0.0.1 -P3306`，能进去即正常。

## 关于景区图片

全部图片都是**本地程序化生成的田园风景图**，放在 `frontend\public\images\`，共 404 张（101 个景区封面 + 303 张图集）。

- 数据库 `scenic_spot.cover_image` / `scenic_image.image_url` 全部指向 `/images/xxx.jpg` 本地路径
- 不依赖 picsum.photos 等任何外部图床，断网、图床宕机都不影响展示
- 新增景区时，把图片放进 `frontend\public\images\` 并把路径写成 `/images/你的文件名.jpg` 即可

## 大模型增强（用户自有密钥模式）

**每个用户用自己的密钥，互不共享，也不会消耗系统所有者的额度。**

配置入口：登录 → 个人中心 → **AI 设置** → 粘贴自己的 API Key → 测试连接 → 保存。

| 模块 | 接口 | 已配置密钥 | 未配置时 |
|---|---|---|---|
| 智能问答「小乡」 | `POST /api/ai/chat` | 知识库检索 + 大模型润色，开放问题也能答 | 知识库原文匹配 |
| AIGC 内容工作台 | `POST /api/admin/content/generate` | 大模型原创文案 | 固定模板拼装 |
| 运营数据分析 | `GET /api/admin/analytics` | 大模型运营建议 | 静态规则结论 |

游客（未登录）始终使用本地引擎。拍照识景（`/api/ai/recognize`）走本地 pHash 引擎，不受影响。

**安全设计**

- 密钥用 AES-256-GCM 加密存库，密钥由 `rural.ai.crypto-secret` 配置（可用环境变量 `AI_CRYPTO_SECRET` 覆盖）
- 数据库只存密文，明文仅在调用大模型时出现在服务端内存
- 所有接口只返回掩码（如 `sk-37***************************445`），实体字段全部 `@JsonIgnore`
- 相关接口：`GET/PUT/DELETE /api/users/ai-config`、`POST /api/users/ai-config/test`（测试不落库）

**管理员可选兜底**：编辑 `start-backend.bat`，取消 `set AI_API_KEY=` 注释可让未配置的用户也走公共密钥（会消耗你的额度，不推荐）。

> 调用失败或额度用尽时后端日志出现「降级本地引擎」，功能自动回退，系统照常运行。

## API 一览（统一 JSON：`{code, message, data}`，code=0 成功）

- 游客：`POST /api/auth/register|login`、`GET /api/scenic-spots[/{id}]`、`GET /api/categories|tags`、`POST /api/ai/chat`、`POST /api/ai/recognize`、`GET /api/scenic-spots/{id}/voice-guide|ratings|similar`、`GET /api/recommendations`
- 登录：`GET|PUT /api/users/profile`、`POST|DELETE /api/favorites/{id}`、`GET /api/favorites`、`POST /api/ratings`、`GET /api/ratings/my`、`GET|POST|DELETE /api/browse-history`
- 管理端（ADMIN）：`/api/admin/users|scenic-spots|categories|tags|ratings|virtual-users|virtual-ratings|knowledge|analytics|content/generate|logs`

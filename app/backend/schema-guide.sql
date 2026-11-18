-- =============================================================
-- 方案三：面向县域文旅的 AI 多模态数字内容生成与智能导览
-- 独立建表脚本，不修改任何存量表（存量 user 表已有手工 ALTER 历史）
-- 全部 CREATE TABLE IF NOT EXISTS，可重复执行
-- 对应权利要求 S1~S6
-- =============================================================

-- S1 县域主体（支撑"轻量化可复制 / 多县域复制"）
CREATE TABLE IF NOT EXISTS `kg_county` (
  `code`        VARCHAR(20) PRIMARY KEY COMMENT '县域编码,如 ZJ-CA-01',
  `name`        VARCHAR(50)  NOT NULL COMMENT '县域名称',
  `province`    VARCHAR(30)  DEFAULT NULL,
  `cover_image` VARCHAR(255) DEFAULT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `status`      TINYINT NOT NULL DEFAULT 1,
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='县域主体(多县域复制)';

-- S1 文旅知识图谱 · 实体（景点/非遗/美食/村落/传承人/技艺）
CREATE TABLE IF NOT EXISTS `kg_entity` (
  `id`            BIGINT PRIMARY KEY AUTO_INCREMENT,
  `county_code`   VARCHAR(20) NOT NULL COMMENT '所属县域',
  `entity_type`   VARCHAR(20) NOT NULL COMMENT 'SCENIC景点/HERITAGE非遗/FOOD美食/VILLAGE村落/PERSON传承人/CRAFT技艺',
  `ref_id`        BIGINT DEFAULT NULL COMMENT '关联 scenic_spot.id(entity_type=SCENIC 时)',
  `name`          VARCHAR(80) NOT NULL,
  `summary`       VARCHAR(500) DEFAULT NULL,
  `detail`        TEXT COMMENT '图文详情/历史沿革',
  `cover_image`   VARCHAR(255) DEFAULT NULL,
  `lng`           DOUBLE DEFAULT NULL COMMENT '经度(S5 位置触发)',
  `lat`           DOUBLE DEFAULT NULL COMMENT '纬度',
  `tags`          VARCHAR(255) DEFAULT NULL COMMENT '标签,逗号分隔',
  `visit_minutes` INT NOT NULL DEFAULT 30 COMMENT '建议游览时长 w_i(分钟)',
  `crowd`         DOUBLE NOT NULL DEFAULT 0.3 COMMENT '拥挤度 c_i,0~1(式1 取负号)',
  `heat`          DOUBLE NOT NULL DEFAULT 0.5 COMMENT '热度 h_i,0~1(式4 指数平滑更新)',
  `content_score` DOUBLE NOT NULL DEFAULT 0 COMMENT '内容完备度 g_i,0~1(由 mm_content 覆盖度算得)',
  `open_time`     VARCHAR(60) DEFAULT NULL,
  `status`        TINYINT NOT NULL DEFAULT 1,
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_county_type` (`county_code`,`entity_type`),
  KEY `idx_ref` (`ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文旅知识图谱实体';

-- S1 文旅知识图谱 · 关系
CREATE TABLE IF NOT EXISTS `kg_relation` (
  `id`            BIGINT PRIMARY KEY AUTO_INCREMENT,
  `source_id`     BIGINT NOT NULL,
  `target_id`     BIGINT NOT NULL,
  `relation_type` VARCHAR(30) NOT NULL COMMENT 'BELONGS_TO隶属/NEARBY邻近/SAME_THEME同主题/RECOMMEND_WITH联游/TRANSFER接驳',
  `weight`        DOUBLE NOT NULL DEFAULT 1 COMMENT '关联强度,0~1',
  `walk_minutes`  INT NOT NULL DEFAULT 0 COMMENT '步行接驳时长 t_ij(分钟,式2)',
  `status`        TINYINT NOT NULL DEFAULT 1,
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_src_tgt_type` (`source_id`,`target_id`,`relation_type`),
  KEY `idx_src` (`source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文旅知识图谱关系';

-- S2 多模态数字内容资源（文本/语音稿/图像/视频脚本 × 风格 × 方言 × 深度档）
CREATE TABLE IF NOT EXISTS `mm_content` (
  `id`            BIGINT PRIMARY KEY AUTO_INCREMENT,
  `entity_id`     BIGINT NOT NULL,
  `content_type`  VARCHAR(20) NOT NULL COMMENT 'TEXT文案/AUDIO语音稿/IMAGE图像/VIDEO视频脚本',
  `style`         VARCHAR(20) NOT NULL DEFAULT '清新' COMMENT '清新/活泼/文艺',
  `dialect`       VARCHAR(20) NOT NULL DEFAULT '普通话' COMMENT '普通话/方言',
  `depth_level`   TINYINT NOT NULL DEFAULT 2 COMMENT '讲解深度档 1简约/2标准/3深度(式3)',
  `title`         VARCHAR(150) DEFAULT NULL,
  `body`          TEXT COMMENT '正文或脚本',
  `media_url`     VARCHAR(255) DEFAULT NULL COMMENT '音频/图片地址',
  `duration_sec`  INT NOT NULL DEFAULT 0 COMMENT '预计时长(秒)',
  `engine`        VARCHAR(20) NOT NULL DEFAULT '本地模板' COMMENT 'AI增强/本地模板',
  `audit_status`  TINYINT NOT NULL DEFAULT 1 COMMENT '1已发布 0待审',
  `exposure`      INT NOT NULL DEFAULT 0 COMMENT '曝光次数',
  `play_count`    INT NOT NULL DEFAULT 0 COMMENT '播放/阅读次数',
  `finish_count`  INT NOT NULL DEFAULT 0 COMMENT '完播/读完次数',
  `score_sum`     INT NOT NULL DEFAULT 0,
  `score_count`   INT NOT NULL DEFAULT 0,
  `score_ema`     DOUBLE NOT NULL DEFAULT 0 COMMENT '满意度 EMA(式4 反馈侧)',
  `created_by`    BIGINT DEFAULT NULL,
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_entity` (`entity_id`,`content_type`),
  KEY `idx_pick` (`entity_id`,`content_type`,`depth_level`,`dialect`,`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多模态数字内容资源';

-- S3 游客画像（千人千面）
CREATE TABLE IF NOT EXISTS `visitor_profile` (
  `id`             BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`        BIGINT NOT NULL,
  `age_band`       VARCHAR(20) DEFAULT '青年' COMMENT '青年/中年/银发/亲子',
  `group_type`     VARCHAR(20) DEFAULT 'solo' COMMENT 'solo/couple/family/friends/team',
  `interests`      VARCHAR(255) DEFAULT NULL COMMENT '兴趣标签,逗号分隔',
  `pace`           VARCHAR(10)  DEFAULT 'normal' COMMENT 'slow/normal/fast',
  `prefer_dialect` VARCHAR(20)  DEFAULT '普通话',
  `depth_level`    TINYINT NOT NULL DEFAULT 2 COMMENT '基础深度档 k_u(式3)',
  `plan_minutes`   INT NOT NULL DEFAULT 240 COMMENT '计划游览时长 T_u(分钟,式2)',
  `avoid_crowd`    TINYINT NOT NULL DEFAULT 1 COMMENT '是否避堵',
  `updated_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游客画像';

-- S4 个性化游览路线
CREATE TABLE IF NOT EXISTS `guide_route` (
  `id`            BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`       BIGINT NOT NULL,
  `county_code`   VARCHAR(20) NOT NULL,
  `name`          VARCHAR(80) DEFAULT NULL,
  `plan_minutes`  INT NOT NULL DEFAULT 240 COMMENT 'T_u',
  `total_minutes` INT NOT NULL DEFAULT 0 COMMENT 'Σw_i + Σt_ij(式2 左式)',
  `total_score`   DOUBLE NOT NULL DEFAULT 0 COMMENT 'Σs_u,i(式1)',
  `item_count`    INT NOT NULL DEFAULT 0,
  `avoid_crowd`   TINYINT NOT NULL DEFAULT 1,
  `status`        VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_user` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个性化游览路线';

-- S4 路线节点
CREATE TABLE IF NOT EXISTS `guide_route_item` (
  `id`             BIGINT PRIMARY KEY AUTO_INCREMENT,
  `route_id`       BIGINT NOT NULL,
  `entity_id`      BIGINT NOT NULL,
  `seq`            INT NOT NULL,
  `arrive_minute`  INT NOT NULL DEFAULT 0 COMMENT '从出发起算的到达分钟',
  `stay_minutes`   INT NOT NULL DEFAULT 0 COMMENT 'w_i',
  `transfer_min`   INT NOT NULL DEFAULT 0 COMMENT 't_ij 接驳时长',
  `score`          DOUBLE NOT NULL DEFAULT 0 COMMENT 's_u,i(式1)',
  `reason`         VARCHAR(200) DEFAULT NULL COMMENT '入选理由(可解释)',
  KEY `idx_route` (`route_id`,`seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路线节点';

-- S5 位置触发自适应讲解会话
CREATE TABLE IF NOT EXISTS `guide_session` (
  `id`            BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`       BIGINT DEFAULT NULL COMMENT '游客可匿名,故可空',
  `route_id`      BIGINT DEFAULT NULL,
  `entity_id`     BIGINT NOT NULL,
  `content_id`    BIGINT DEFAULT NULL,
  `trigger_lng`   DOUBLE DEFAULT NULL,
  `trigger_lat`   DOUBLE DEFAULT NULL,
  `distance_m`    INT DEFAULT NULL COMMENT '触发距离(米)',
  `depth_level`   TINYINT NOT NULL DEFAULT 2 COMMENT 'd_u(式3)',
  `depth_detail`  VARCHAR(120) DEFAULT NULL COMMENT '判定明细,便于解释',
  `dwell_seconds` INT NOT NULL DEFAULT 0 COMMENT '停留秒数(式3 的 r_u)',
  `ask_count`     INT NOT NULL DEFAULT 0 COMMENT '已提问次数(式3 的 q_u)',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_user` (`user_id`,`created_at`),
  KEY `idx_entity` (`entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='位置触发导览会话';

-- S6 反馈闭环
CREATE TABLE IF NOT EXISTS `guide_feedback` (
  `id`         BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`    BIGINT NOT NULL,
  `entity_id`  BIGINT NOT NULL,
  `content_id` BIGINT DEFAULT NULL,
  `score`      INT NOT NULL COMMENT '1~5',
  `comment`    VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_content` (`user_id`,`content_id`),
  KEY `idx_entity` (`entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导览反馈(驱动式4)';

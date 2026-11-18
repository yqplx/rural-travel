-- =============================================================
-- 轻量化AI乡村文旅智能引流与服务系统 数据库结构 (MySQL 8.0)
-- 全部使用 CREATE TABLE IF NOT EXISTS，可重复执行
-- =============================================================

CREATE TABLE IF NOT EXISTS `user` (
  `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username`    VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录名',
  `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt哈希',
  `nickname`    VARCHAR(50)  DEFAULT NULL,
  `email`       VARCHAR(100) DEFAULT NULL,
  `phone`       VARCHAR(20)  DEFAULT NULL,
  `avatar`      VARCHAR(255) DEFAULT NULL,
  `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `province`    VARCHAR(30)  DEFAULT NULL COMMENT '所在省份(客源分析用)',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `category` (
  `id`         BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name`       VARCHAR(30) NOT NULL UNIQUE,
  `icon`       VARCHAR(50) DEFAULT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `status`     TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景区分类';

CREATE TABLE IF NOT EXISTS `tag` (
  `id`         BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name`       VARCHAR(30) NOT NULL UNIQUE,
  `sort_order` INT NOT NULL DEFAULT 0,
  `status`     TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景区标签';

CREATE TABLE IF NOT EXISTS `scenic_spot` (
  `id`           BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name`         VARCHAR(80) NOT NULL,
  `category_id`  BIGINT NOT NULL,
  `tag_ids`      VARCHAR(255) DEFAULT NULL COMMENT '标签id,逗号分隔',
  `province`     VARCHAR(30) DEFAULT NULL,
  `city`         VARCHAR(50) DEFAULT NULL,
  `address`      VARCHAR(150) DEFAULT NULL,
  `description`  TEXT COMMENT '景区介绍',
  `features`     VARCHAR(500) DEFAULT NULL COMMENT '特色亮点',
  `tips`         VARCHAR(500) DEFAULT NULL COMMENT '游览贴士',
  `ticket_price` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `open_time`    VARCHAR(60) DEFAULT NULL,
  `cover_image`  VARCHAR(255) DEFAULT NULL,
  `avg_score`    DECIMAL(3,1) NOT NULL DEFAULT 0,
  `score_count`  INT NOT NULL DEFAULT 0,
  `view_count`   INT NOT NULL DEFAULT 0,
  `status`       TINYINT NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_category` (`category_id`),
  KEY `idx_price` (`ticket_price`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景区表';

CREATE TABLE IF NOT EXISTS `scenic_image` (
  `id`             BIGINT PRIMARY KEY AUTO_INCREMENT,
  `scenic_spot_id` BIGINT NOT NULL,
  `image_url`      VARCHAR(255) NOT NULL,
  `sort_order`     INT NOT NULL DEFAULT 0,
  KEY `idx_spot` (`scenic_spot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景区图片';

CREATE TABLE IF NOT EXISTS `user_interest` (
  `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`     BIGINT NOT NULL,
  `category_id` BIGINT DEFAULT NULL,
  `tag_id`      BIGINT DEFAULT NULL,
  `weight`      DOUBLE NOT NULL DEFAULT 0,
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_cat_tag` (`user_id`,`category_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户兴趣画像';

CREATE TABLE IF NOT EXISTS `rating` (
  `id`             BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`        BIGINT NOT NULL,
  `scenic_spot_id` BIGINT NOT NULL,
  `score`          INT NOT NULL COMMENT '1-5',
  `comment`        VARCHAR(500) DEFAULT NULL,
  `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_spot` (`user_id`,`scenic_spot_id`),
  KEY `idx_spot` (`scenic_spot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户评分';

CREATE TABLE IF NOT EXISTS `favorite` (
  `id`             BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`        BIGINT NOT NULL,
  `scenic_spot_id` BIGINT NOT NULL,
  `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_spot` (`user_id`,`scenic_spot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏';

CREATE TABLE IF NOT EXISTS `browse_history` (
  `id`             BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`        BIGINT NOT NULL,
  `scenic_spot_id` BIGINT NOT NULL,
  `duration`       INT NOT NULL DEFAULT 0 COMMENT '停留秒数',
  `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_user_time` (`user_id`,`created_at`),
  KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览记录';

CREATE TABLE IF NOT EXISTS `virtual_user` (
  `id`            BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name`          VARCHAR(50) NOT NULL,
  `interest_tags` VARCHAR(255) DEFAULT NULL COMMENT '兴趣标签id,逗号分隔',
  `description`   VARCHAR(255) DEFAULT NULL,
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟用户(推荐样本)';

CREATE TABLE IF NOT EXISTS `virtual_rating` (
  `id`              BIGINT PRIMARY KEY AUTO_INCREMENT,
  `virtual_user_id` BIGINT NOT NULL,
  `scenic_spot_id`  BIGINT NOT NULL,
  `score`           INT NOT NULL,
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_vuser` (`virtual_user_id`),
  KEY `idx_spot` (`scenic_spot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟用户评分样本';

CREATE TABLE IF NOT EXISTS `recommend_result` (
  `id`             BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`        BIGINT NOT NULL,
  `scenic_spot_id` BIGINT NOT NULL,
  `score`          DOUBLE NOT NULL DEFAULT 0,
  `reason`         VARCHAR(120) DEFAULT NULL COMMENT '推荐语(自然化)',
  `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_user` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐结果缓存';

CREATE TABLE IF NOT EXISTS `sys_log` (
  `id`         BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`    BIGINT DEFAULT NULL,
  `username`   VARCHAR(50) DEFAULT NULL,
  `action`     VARCHAR(50) NOT NULL,
  `detail`     VARCHAR(500) DEFAULT NULL,
  `ip`         VARCHAR(50) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志';

-- ============ AI 模块新增表 ============
CREATE TABLE IF NOT EXISTS `knowledge_entry` (
  `id`         BIGINT PRIMARY KEY AUTO_INCREMENT,
  `question`   VARCHAR(255) NOT NULL,
  `answer`     TEXT NOT NULL,
  `keywords`   VARCHAR(255) DEFAULT NULL COMMENT '检索词,逗号分隔',
  `category`   VARCHAR(30) DEFAULT '通用',
  `sort_order` INT NOT NULL DEFAULT 0,
  `status`     TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答知识库';

CREATE TABLE IF NOT EXISTS `scenic_image_hash` (
  `id`             BIGINT PRIMARY KEY AUTO_INCREMENT,
  `scenic_spot_id` BIGINT NOT NULL,
  `image_url`      VARCHAR(255) DEFAULT NULL,
  `hash_value`     VARCHAR(32) NOT NULL COMMENT '64位pHash(16进制)',
  `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_spot` (`scenic_spot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='识景图像哈希缓存';

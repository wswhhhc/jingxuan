-- ============================================================
-- 迁移：作品点赞/收藏 + 浏览计数 + 标签系统
-- ============================================================

-- 1. 作品浏览计数字段
ALTER TABLE work ADD COLUMN view_count INT NOT NULL DEFAULT 0 COMMENT '浏览数' AFTER batch_id;

-- 2. 作品点赞表
CREATE TABLE IF NOT EXISTS work_like (
    id BIGINT NOT NULL COMMENT '点赞ID',
    work_id BIGINT NOT NULL COMMENT '作品ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_user (work_id, user_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品点赞';

-- 3. 作品点赞计数字段
ALTER TABLE work ADD COLUMN like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER view_count;

-- 4. 标签表
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT NOT NULL COMMENT '标签ID',
    name VARCHAR(50) NOT NULL COMMENT '标签名称',
    color VARCHAR(20) DEFAULT '#409EFF' COMMENT '标签颜色',
    type VARCHAR(30) DEFAULT 'tech' COMMENT '标签类型：tech=技术栈 category=分类',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name),
    KEY idx_type (type),
    KEY idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签';

-- 5. 作品-标签关联表
CREATE TABLE IF NOT EXISTS work_tag (
    id BIGINT NOT NULL COMMENT '关联ID',
    work_id BIGINT NOT NULL COMMENT '作品ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_tag (work_id, tag_id),
    KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品标签关联';

-- 6. 迁移现有 tech_stack 数据到标签表
INSERT INTO tag (id, name, type, sort)
SELECT
    ROW_NUMBER() OVER () + 10000,
    TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(t.tech, ',', n.n), ',', -1)),
    'tech',
    0
FROM (SELECT DISTINCT tech_stack AS tech FROM work WHERE tech_stack IS NOT NULL AND tech_stack != '') t
CROSS JOIN (
    SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) n
WHERE n.n <= LENGTH(t.tech) - LENGTH(REPLACE(t.tech, ',', '')) + 1
  AND TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(t.tech, ',', n.n), ',', -1)) != ''
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 7. 建立已有作品的标签关联
INSERT INTO work_tag (id, work_id, tag_id)
SELECT
    ROW_NUMBER() OVER () + 20000,
    w.id,
    t.id
FROM work w
JOIN tag t ON FIND_IN_SET(t.name, w.tech_stack) > 0
WHERE w.tech_stack IS NOT NULL AND w.tech_stack != ''
  AND w.deleted = 0
  AND t.deleted = 0
ON DUPLICATE KEY UPDATE work_tag.work_id = work_tag.work_id;

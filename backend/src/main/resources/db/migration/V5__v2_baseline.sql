-- ============================================================
-- V5: v2 基线扩展表
-- 说明: v2 重构新增的独立表结构。这些表在 v1 中可能以不同
--       形式存在（如 sys_dict 中的 class 类型、work.tech_stack
--       逗号分隔、work_attachment 作附件管理、reward_config +
--       rank_reward 作奖项、score_batch.class_scopes JSON），
--       V5 将其规范化到新表中。
--
-- 运行场景: 仅应用于新数据库（如 jingxuan_v2），Flyway 保证
--           幂等性（CREATE TABLE IF NOT EXISTS）。
--
-- V1 基线已创建的表: sys_role, sys_user, sys_menu,
--   sys_role_menu, sys_dict, sys_log, sys_notice,
--   sys_notification, work, work_member, work_attachment,
--   work_audit, work_publish, work_score, work_comment,
--   score_batch, sensitive_rule, rank_reward, reward_issue,
--   reward_config, work_like, tag, work_tag, student_task,
--   delete_request
-- ============================================================

-- ============================================================
-- 1. academic_class — 班级表
--    替代 sys_dict 中 dict_type='class' 的数据，结构更规范。
-- ============================================================
CREATE TABLE IF NOT EXISTS academic_class (
    id BIGINT NOT NULL PRIMARY KEY,
    class_name VARCHAR(100) NOT NULL COMMENT '班级名称',
    class_code VARCHAR(50) NOT NULL COMMENT '班级代码',
    grade VARCHAR(20) DEFAULT NULL COMMENT '年级',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- ============================================================
-- 2. file_asset — 文件资产
--    独立存储文件元信息，与 work_attachment 解耦。
--    一个 file_asset 可被多条 work_attachment 引用。
-- ============================================================
CREATE TABLE IF NOT EXISTS file_asset (
    id BIGINT NOT NULL PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    stored_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256 CHAR(64) DEFAULT NULL,
    mime_type VARCHAR(100) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stored_path (stored_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件资产';

-- ============================================================
-- 3. award_tier — 奖项配置
--    替代 reward_config + rank_reward 两张表，合并为统一
--    的奖项等级配置。
-- ============================================================
CREATE TABLE IF NOT EXISTS award_tier (
    id BIGINT NOT NULL PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    tier_name VARCHAR(100) NOT NULL COMMENT '奖项等级名称，如一等奖',
    prize_name VARCHAR(200) DEFAULT NULL COMMENT '奖品说明',
    quota INT NOT NULL DEFAULT 0 COMMENT '名额',
    sort INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_batch_id (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖项配置';

-- ============================================================
-- 4. score_batch_class — 评分批次班级范围
--    替代 score_batch.class_scopes JSON 字段，用关联表
--    实现规范化存储。
-- ============================================================
CREATE TABLE IF NOT EXISTS score_batch_class (
    id BIGINT NOT NULL PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    UNIQUE KEY uk_batch_class (batch_id, class_id),
    KEY idx_class_id (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分批次班级范围';

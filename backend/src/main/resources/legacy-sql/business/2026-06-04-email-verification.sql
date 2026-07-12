-- ============================================================
-- 邮箱验证注册功能支持
-- 1. 添加 (email, role_id) 联合唯一索引
-- 2. 同一邮箱可注册学生和教师各一个账号
-- ============================================================

USE jingxuan;

SET @idx_exists := (SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'jingxuan'
      AND TABLE_NAME = 'sys_user'
      AND INDEX_NAME = 'uk_email_role');

SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE sys_user ADD UNIQUE INDEX uk_email_role (email(100), role_id)',
    'SELECT ''Index uk_email_role already exists''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

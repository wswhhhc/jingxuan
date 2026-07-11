-- ============================================================
-- 修复缺少 deleted 列的问题
-- BaseEntity 定义了 @TableLogic 逻辑删除字段，
-- 但 sys_notification 和 sys_log 表缺少 deleted 列，
-- 导致 MyBatis-Plus 自动添加 AND deleted=0 时报 Unknown column 错误
-- ============================================================

ALTER TABLE sys_notification
    ADD COLUMN deleted tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    ADD INDEX idx_deleted (deleted);

ALTER TABLE sys_log
    ADD COLUMN deleted tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    ADD INDEX idx_deleted (deleted);

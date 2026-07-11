-- 公告表增加通知发送范围字段
ALTER TABLE sys_notice
    ADD COLUMN target_scope VARCHAR(20) DEFAULT 'all' NOT NULL COMMENT '通知发送范围：student=仅学生  teacher=仅教师  all=全体';

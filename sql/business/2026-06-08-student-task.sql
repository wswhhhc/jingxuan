-- 学生待办任务表
-- 管理员创建评分批次时，发布待办任务给班级范围内的学生
-- 学生查看待办列表，选择一项后提交作品

CREATE TABLE IF NOT EXISTS student_task (
    id          BIGINT        NOT NULL COMMENT '雪花ID',
    user_id     BIGINT        NOT NULL COMMENT '学生用户ID',
    batch_id    BIGINT        NOT NULL COMMENT '关联评分批次ID',
    work_id     BIGINT        DEFAULT NULL COMMENT '关联作品ID（提交后回填）',
    title       VARCHAR(200)  DEFAULT NULL COMMENT '待办标题',
    content     TEXT          DEFAULT NULL COMMENT '待办要求说明',
    status      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0=待处理 1=已完成 2=已驳回 3=已截止',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
    PRIMARY KEY (id) USING BTREE,
    KEY idx_user_id (user_id) USING BTREE,
    KEY idx_batch_id (batch_id) USING BTREE,
    KEY idx_user_batch (user_id, batch_id) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生待办任务表';

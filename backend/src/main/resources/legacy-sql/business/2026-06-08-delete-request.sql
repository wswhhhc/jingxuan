-- 作品删除申请表
-- 学生申请删除已审核通过的作品，管理员审批后执行实际删除

CREATE TABLE IF NOT EXISTS delete_request (
    id          BIGINT        NOT NULL COMMENT '雪花ID',
    work_id     BIGINT        NOT NULL COMMENT '申请删除的作品ID',
    student_id  BIGINT        NOT NULL COMMENT '申请人（学生）ID',
    reason      VARCHAR(500)  NOT NULL COMMENT '申请原因',
    status      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0=待处理 1=已同意 2=已拒绝',
    admin_reply VARCHAR(500)  DEFAULT NULL COMMENT '管理员回复（拒绝时填原因）',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
    PRIMARY KEY (id) USING BTREE,
    KEY idx_work_id (work_id) USING BTREE,
    KEY idx_student_id (student_id) USING BTREE,
    KEY idx_status (status) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='作品删除申请表';

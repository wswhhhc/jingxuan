-- H2 兼容模式（MySQL）建表脚本
-- 注意：H2 会忽略 ENGINE=InnoDB, CHARSET=utf8mb4 等 MySQL 特有语法

-- ============================================================
-- 基础表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    role_id INT NOT NULL,
    class_id BIGINT,
    status INT DEFAULT 1,
    first_login INT DEFAULT 1,
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT,
    menu_name VARCHAR(100) NOT NULL,
    menu_type INT DEFAULT 0,
    path VARCHAR(200),
    component VARCHAR(200),
    perms VARCHAR(200),
    icon VARCHAR(100),
    sort INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT PRIMARY KEY,
    dict_type VARCHAR(50) NOT NULL,
    dict_label VARCHAR(100) NOT NULL,
    dict_value VARCHAR(100) NOT NULL,
    sort INT DEFAULT 0,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    operation VARCHAR(200),
    method VARCHAR(200),
    params TEXT,
    ip VARCHAR(50),
    duration BIGINT,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_notice (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    type INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_notification (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    content TEXT,
    type VARCHAR(50),
    is_read INT DEFAULT 0,
    related_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

-- ============================================================
-- 业务表
-- ============================================================
CREATE TABLE IF NOT EXISTS score_batch (
    id BIGINT PRIMARY KEY,
    batch_name VARCHAR(200) NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    class_scopes TEXT,
    status INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS work (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    summary TEXT,
    tech_stack VARCHAR(500),
    advisor VARCHAR(50),
    cover_url VARCHAR(500),
    video_url VARCHAR(500),
    preview_url VARCHAR(500),
    run_desc TEXT,
    status INT DEFAULT 0,
    reject_reason VARCHAR(500),
    submitter_id BIGINT,
    submit_time DATETIME,
    batch_id BIGINT,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS work_member (
    id BIGINT PRIMARY KEY,
    work_id BIGINT NOT NULL,
    student_id BIGINT,
    student_name VARCHAR(50),
    student_no VARCHAR(50),
    class_name VARCHAR(100),
    is_leader INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS work_attachment (
    id BIGINT PRIMARY KEY,
    work_id BIGINT,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    file_url VARCHAR(500) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS work_audit (
    id BIGINT PRIMARY KEY,
    work_id BIGINT NOT NULL,
    auditor_id BIGINT,
    audit_status INT NOT NULL,
    audit_comment VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS work_publish (
    id BIGINT PRIMARY KEY,
    work_id BIGINT NOT NULL,
    publish_status INT DEFAULT 0,
    featured INT DEFAULT 0,
    publish_time DATETIME,
    publisher_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS work_score (
    id BIGINT PRIMARY KEY,
    work_id BIGINT NOT NULL,
    scorer_id BIGINT NOT NULL,
    score DECIMAL(5,2),
    innovation_score DECIMAL(5,2),
    tech_score DECIMAL(5,2),
    completion_score DECIMAL(5,2),
    practicality_score DECIMAL(5,2),
    comment VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS work_comment (
    id BIGINT PRIMARY KEY,
    work_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sensitive_rule (
    id BIGINT PRIMARY KEY,
    rule_name VARCHAR(200) NOT NULL,
    rule_type VARCHAR(50),
    content TEXT,
    action VARCHAR(50) DEFAULT 'reject',
    enabled INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS rank_reward (
    id BIGINT PRIMARY KEY,
    batch_id BIGINT,
    rank_from INT,
    rank_to INT,
    reward_name VARCHAR(200),
    reward_desc TEXT,
    sort INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS reward_issue (
    id BIGINT PRIMARY KEY,
    work_id BIGINT,
    batch_id BIGINT,
    student_id BIGINT,
    reward_id BIGINT,
    status INT DEFAULT 0,
    issue_time DATETIME,
    operator_id BIGINT,
    cancel_time DATETIME,
    cancel_reason VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS reward_config (
    id BIGINT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    description VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS work_like (
    id BIGINT PRIMARY KEY,
    work_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(20),
    type VARCHAR(50),
    sort INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS work_tag (
    id BIGINT PRIMARY KEY,
    work_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

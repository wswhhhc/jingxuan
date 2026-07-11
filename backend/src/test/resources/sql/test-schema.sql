-- ============================================================
-- 集成测试 Schema（MySQL 8，由 Testcontainers 提供）
-- 仅维护当前 API 集成测试所需的 v1 表结构，不作为生产建表来源。
-- ============================================================

-- 角色
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
);

-- 用户
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    role_id INT NOT NULL,
    class_id BIGINT DEFAULT NULL,
    avatar VARCHAR(255) DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    first_login TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email_role_deleted (email, role_id, deleted),
    KEY idx_role_id (role_id),
    KEY idx_class_id (class_id)
);

-- 菜单
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT NOT NULL,
    menu_name VARCHAR(50) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    path VARCHAR(200) DEFAULT NULL,
    permission VARCHAR(200) DEFAULT NULL,
    type TINYINT NOT NULL DEFAULT 1,
    icon VARCHAR(100) DEFAULT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
);

-- 角色-菜单
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    KEY idx_menu_id (menu_id)
);

-- 字典
CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT NOT NULL,
    dict_type VARCHAR(50) NOT NULL,
    dict_label VARCHAR(100) NOT NULL,
    dict_value VARCHAR(100) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    remark VARCHAR(255) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_dict_type (dict_type)
);

-- 日志
CREATE TABLE IF NOT EXISTS sys_log (
    id BIGINT NOT NULL,
    user_id BIGINT DEFAULT NULL,
    username VARCHAR(50) DEFAULT NULL,
    action VARCHAR(100) NOT NULL,
    target VARCHAR(255) DEFAULT NULL,
    target_id BIGINT DEFAULT NULL,
    ip VARCHAR(50) DEFAULT NULL,
    request_method VARCHAR(10) DEFAULT NULL,
    request_path VARCHAR(255) DEFAULT NULL,
    params TEXT DEFAULT NULL,
    result TINYINT NOT NULL DEFAULT 1,
    error_msg VARCHAR(500) DEFAULT NULL,
    duration BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_action (action),
    KEY idx_target_id (target_id),
    KEY idx_create_time (create_time)
);

-- 公告
CREATE TABLE IF NOT EXISTS sys_notice (
    id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    publisher_id BIGINT NOT NULL,
    publish_time DATETIME DEFAULT NULL,
    top_flag TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0,
    target_scope VARCHAR(20) NOT NULL DEFAULT 'all',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_publisher_id (publisher_id),
    KEY idx_publish_time (publish_time)
);

-- 通知
CREATE TABLE IF NOT EXISTS sys_notification (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT DEFAULT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ref_id BIGINT DEFAULT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    read_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_is_read (is_read),
    KEY idx_create_time (create_time)
);

-- ==================== 业务表 ====================

-- 作品
CREATE TABLE IF NOT EXISTS work (
    id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary TEXT DEFAULT NULL,
    tech_stack VARCHAR(500) DEFAULT NULL,
    advisor VARCHAR(100) DEFAULT NULL,
    cover_url VARCHAR(500) DEFAULT NULL,
    video_url VARCHAR(500) DEFAULT NULL,
    preview_url VARCHAR(500) DEFAULT NULL,
    run_desc TEXT DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    submitter_id BIGINT NOT NULL,
    submit_time DATETIME DEFAULT NULL,
    batch_id BIGINT DEFAULT NULL,
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submitter_id (submitter_id),
    KEY idx_status (status),
    KEY idx_batch_id (batch_id),
    KEY idx_submit_time (submit_time),
    KEY idx_tech_stack (tech_stack(64))
);

-- 成员
CREATE TABLE IF NOT EXISTS work_member (
    id BIGINT NOT NULL,
    work_id BIGINT NOT NULL,
    student_id BIGINT DEFAULT NULL,
    student_name VARCHAR(50) NOT NULL,
    student_no VARCHAR(50) NOT NULL,
    class_name VARCHAR(100) NOT NULL,
    is_leader TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_work_id (work_id),
    KEY idx_student_id (student_id),
    KEY idx_is_leader (is_leader)
);

-- 附件
CREATE TABLE IF NOT EXISTS work_attachment (
    id BIGINT NOT NULL,
    work_id BIGINT DEFAULT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    file_url VARCHAR(500) NOT NULL,
    category VARCHAR(20) DEFAULT 'attachment',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_work_id (work_id),
    KEY idx_category (category)
);

-- 审核
CREATE TABLE IF NOT EXISTS work_audit (
    id BIGINT NOT NULL,
    work_id BIGINT NOT NULL,
    auditor_id BIGINT NOT NULL,
    result TINYINT NOT NULL,
    reason VARCHAR(500) DEFAULT NULL,
    audit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_work_id (work_id),
    KEY idx_auditor_id (auditor_id),
    KEY idx_audit_time (audit_time)
);

-- 发布
CREATE TABLE IF NOT EXISTS work_publish (
    id BIGINT NOT NULL,
    work_id BIGINT NOT NULL,
    publish_status TINYINT NOT NULL DEFAULT 0,
    featured TINYINT NOT NULL DEFAULT 0,
    publish_time DATETIME DEFAULT NULL,
    offline_time DATETIME DEFAULT NULL,
    publisher_id BIGINT DEFAULT NULL,
    preview_url VARCHAR(500) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_id (work_id),
    KEY idx_publish_status (publish_status),
    KEY idx_featured (featured),
    KEY idx_publish_time (publish_time)
);

-- 评分
CREATE TABLE IF NOT EXISTS work_score (
    id BIGINT NOT NULL,
    work_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    innovation DECIMAL(5,2) NOT NULL DEFAULT 0,
    difficulty DECIMAL(5,2) NOT NULL DEFAULT 0,
    completion DECIMAL(5,2) NOT NULL DEFAULT 0,
    practicality DECIMAL(5,2) NOT NULL DEFAULT 0,
    total DECIMAL(5,2) NOT NULL DEFAULT 0,
    comment TEXT DEFAULT NULL,
    batch_id BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_teacher (work_id, teacher_id),
    KEY idx_teacher_id (teacher_id),
    KEY idx_batch_id (batch_id),
    KEY idx_total (total)
);

-- 评论
CREATE TABLE IF NOT EXISTS work_comment (
    id BIGINT NOT NULL,
    work_id BIGINT NOT NULL,
    user_id BIGINT DEFAULT NULL,
    guest_name VARCHAR(50) DEFAULT NULL,
    content TEXT NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_work_id (work_id),
    KEY idx_user_id (user_id),
    KEY idx_parent_id (parent_id)
);

-- 评分批次
CREATE TABLE IF NOT EXISTS score_batch (
    id BIGINT NOT NULL,
    batch_name VARCHAR(200) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    class_scopes TEXT DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    rank_published TINYINT NOT NULL DEFAULT 0,
    notice_title VARCHAR(200) DEFAULT NULL,
    notice_content TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_start_time (start_time),
    KEY idx_end_time (end_time)
);

-- 学生待办
CREATE TABLE IF NOT EXISTS student_task (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    work_id BIGINT DEFAULT NULL,
    title VARCHAR(200) DEFAULT NULL,
    content TEXT DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_batch_id (batch_id),
    KEY idx_user_batch (user_id, batch_id)
);

-- 作品删除申请
CREATE TABLE IF NOT EXISTS delete_request (
    id BIGINT NOT NULL,
    work_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    admin_reply VARCHAR(500) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_work_id (work_id),
    KEY idx_student_id (student_id),
    KEY idx_status (status)
);

-- 敏感规则
CREATE TABLE IF NOT EXISTS sensitive_rule (
    id BIGINT NOT NULL,
    rule_name VARCHAR(200) NOT NULL,
    system_prompt TEXT NOT NULL,
    enabled_categories TEXT DEFAULT NULL,
    on_reject_action VARCHAR(50) NOT NULL DEFAULT 'reject',
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status (status)
);

-- 奖项配置
CREATE TABLE IF NOT EXISTS reward_config (
    id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    reward_level VARCHAR(20) NOT NULL,
    reward_name VARCHAR(100) NOT NULL DEFAULT '',
    prize_name VARCHAR(200) NOT NULL DEFAULT '',
    quota INT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_batch_id (batch_id)
);

-- 排名奖励配置
CREATE TABLE IF NOT EXISTS rank_reward (
    id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    reward_level INT NOT NULL,
    reward_name VARCHAR(100) NOT NULL DEFAULT '',
    prize_name VARCHAR(200) DEFAULT NULL,
    prize_image VARCHAR(500) DEFAULT NULL,
    quota INT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_batch_id (batch_id),
    KEY idx_reward_level (reward_level)
);

-- 奖品发放
CREATE TABLE IF NOT EXISTS reward_issue (
    id BIGINT NOT NULL,
    reward_id BIGINT NOT NULL,
    work_id BIGINT NOT NULL,
    issue_status TINYINT NOT NULL DEFAULT 0,
    issue_time DATETIME DEFAULT NULL,
    operator_id BIGINT DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_reward_id (reward_id),
    KEY idx_work_id (work_id),
    KEY idx_issue_status (issue_status)
);

-- 标签
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(20) DEFAULT '#409EFF',
    type VARCHAR(30) DEFAULT 'tech',
    sort INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
);

-- 作品-标签关联
CREATE TABLE IF NOT EXISTS work_tag (
    id BIGINT NOT NULL,
    work_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_tag (work_id, tag_id)
);

-- 点赞
CREATE TABLE IF NOT EXISTS work_like (
    id BIGINT NOT NULL,
    work_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_user (work_id, user_id),
    KEY idx_user_id (user_id)
);

-- ============================================================
-- 学院作品展示平台 - 业务表结构
-- 版本: 1.0
-- 说明: 作品、审核、发布、评分、排行、评论等业务表
-- ============================================================

USE jingxuan;

-- ============================================================
-- 1. 作品主表
-- ============================================================
CREATE TABLE IF NOT EXISTS work (
    id BIGINT NOT NULL COMMENT '作品ID',
    title VARCHAR(200) NOT NULL COMMENT '作品名称',
    summary TEXT DEFAULT NULL COMMENT '作品简介',
    tech_stack VARCHAR(500) DEFAULT NULL COMMENT '技术栈，逗号分隔',
    advisor VARCHAR(100) DEFAULT NULL COMMENT '指导教师',
    cover_url VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    video_url VARCHAR(500) DEFAULT NULL COMMENT '上传的mp4演示视频文件地址',
    preview_url VARCHAR(500) DEFAULT NULL COMMENT '服务器访问地址',
    run_desc TEXT DEFAULT NULL COMMENT '运行说明',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态 0=草稿 1=已提交 2=已驳回 3=已通过',
    submitter_id BIGINT NOT NULL COMMENT '提交人/队长ID',
    submit_time DATETIME DEFAULT NULL COMMENT '提交时间',
    batch_id BIGINT DEFAULT NULL COMMENT '关联评分批次ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_submitter_id (submitter_id),
    KEY idx_status (status),
    KEY idx_batch_id (batch_id),
    KEY idx_submit_time (submit_time),
    KEY idx_tech_stack (tech_stack(64))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品主表';

-- ============================================================
-- 2. 作品成员表
-- ============================================================
CREATE TABLE IF NOT EXISTS work_member (
    id BIGINT NOT NULL COMMENT '成员ID',
    work_id BIGINT NOT NULL COMMENT '作品ID',
    student_id BIGINT DEFAULT NULL COMMENT '学生用户ID（已注册用户）',
    student_name VARCHAR(50) NOT NULL COMMENT '学生姓名',
    student_no VARCHAR(50) NOT NULL COMMENT '学号',
    class_name VARCHAR(100) NOT NULL COMMENT '班级名称',
    is_leader TINYINT NOT NULL DEFAULT 0 COMMENT '是否队长 0=否 1=是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_work_id (work_id),
    KEY idx_student_id (student_id),
    KEY idx_is_leader (is_leader)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品成员';

-- ============================================================
-- 3. 作品附件表
-- ============================================================
CREATE TABLE IF NOT EXISTS work_attachment (
    id BIGINT NOT NULL COMMENT '附件ID',
    work_id BIGINT DEFAULT NULL COMMENT '作品ID（上传时可为空，创建作品后再关联）',
    file_name VARCHAR(255) NOT NULL COMMENT '原文件名',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型，如 zip / jpg / mp4',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    file_url VARCHAR(500) NOT NULL COMMENT '文件存储路径/URL',
    category VARCHAR(20) DEFAULT 'attachment' COMMENT '附件分类：attachment=压缩包 screenshot=截图 video=视频 other=其他',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_work_id (work_id),
    KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品附件';

-- ============================================================
-- 4. 作品审核表
-- ============================================================
CREATE TABLE IF NOT EXISTS work_audit (
    id BIGINT NOT NULL COMMENT '审核记录ID',
    work_id BIGINT NOT NULL COMMENT '作品ID',
    auditor_id BIGINT NOT NULL COMMENT '审核人ID',
    result TINYINT NOT NULL COMMENT '审核结果 0=驳回 1=通过',
    reason VARCHAR(500) DEFAULT NULL COMMENT '驳回原因/审核意见',
    audit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_work_id (work_id),
    KEY idx_auditor_id (auditor_id),
    KEY idx_audit_time (audit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品审核记录';

-- ============================================================
-- 5. 作品发布表
-- ============================================================
CREATE TABLE IF NOT EXISTS work_publish (
    id BIGINT NOT NULL COMMENT '发布记录ID',
    work_id BIGINT NOT NULL COMMENT '作品ID',
    publish_status TINYINT NOT NULL DEFAULT 0 COMMENT '发布状态 0=未发布 1=已发布 2=已下线',
    featured TINYINT NOT NULL DEFAULT 0 COMMENT '精选标记 0=普通 1=精选',
    publish_time DATETIME DEFAULT NULL COMMENT '发布时间',
    offline_time DATETIME DEFAULT NULL COMMENT '下线时间',
    publisher_id BIGINT DEFAULT NULL COMMENT '发布人ID',
    preview_url VARCHAR(500) DEFAULT NULL COMMENT '在线体验地址',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_id (work_id),
    KEY idx_publish_status (publish_status),
    KEY idx_featured (featured),
    KEY idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品发布';

-- ============================================================
-- 6. 作品评分表
-- ============================================================
CREATE TABLE IF NOT EXISTS work_score (
    id BIGINT NOT NULL COMMENT '评分ID',
    work_id BIGINT NOT NULL COMMENT '作品ID',
    teacher_id BIGINT NOT NULL COMMENT '评分教师ID',
    innovation DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '创新性（满分25）',
    difficulty DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '技术难度（满分25）',
    completion DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '完成度（满分30）',
    practicality DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '实用性（满分20）',
    total DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '总分',
    comment TEXT DEFAULT NULL COMMENT '教师评语',
    batch_id BIGINT DEFAULT NULL COMMENT '评分批次ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_teacher (work_id, teacher_id),
    KEY idx_teacher_id (teacher_id),
    KEY idx_batch_id (batch_id),
    KEY idx_total (total)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品评分';

-- ============================================================
-- 7. 作品评论表（P1）
-- ============================================================
CREATE TABLE IF NOT EXISTS work_comment (
    id BIGINT NOT NULL COMMENT '评论ID',
    work_id BIGINT NOT NULL COMMENT '作品ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    parent_id BIGINT DEFAULT NULL COMMENT '父评论ID（回复用）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_work_id (work_id),
    KEY idx_user_id (user_id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品评论';

-- ============================================================
-- 8. 评分批次表
-- ============================================================
CREATE TABLE IF NOT EXISTS score_batch (
    id BIGINT NOT NULL COMMENT '批次ID',
    batch_name VARCHAR(200) NOT NULL COMMENT '批次名称，如 2025年春学期实训评分',
    start_time DATETIME NOT NULL COMMENT '评分开始时间',
    end_time DATETIME NOT NULL COMMENT '评分结束时间',
    class_scopes TEXT DEFAULT NULL COMMENT '适用班级范围，JSON数组',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0=未开始 1=进行中 2=已结束',
    rank_published TINYINT NOT NULL DEFAULT 0 COMMENT '排行榜是否已公示 0=未公示 1=已公示',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_start_time (start_time),
    KEY idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分批次';

-- ============================================================
-- 9. 内容审核规则表
-- ============================================================
CREATE TABLE IF NOT EXISTS sensitive_rule (
    id BIGINT NOT NULL COMMENT '规则ID',
    rule_name VARCHAR(200) NOT NULL COMMENT '规则名称',
    system_prompt TEXT NOT NULL COMMENT 'LLM审核System Prompt',
    enabled_categories TEXT DEFAULT NULL COMMENT '启用的违规类别，JSON数组',
    on_reject_action VARCHAR(50) NOT NULL DEFAULT 'reject' COMMENT '检测不通过处理策略：reject=拒绝 warning=警告 review=人工复核',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态 0=禁用 1=启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容审核规则';

-- ============================================================
-- 10. 奖项配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS rank_reward (
    id BIGINT NOT NULL COMMENT '奖项ID',
    batch_id BIGINT NOT NULL COMMENT '评分批次ID',
    reward_level TINYINT NOT NULL COMMENT '奖项级别 1=一等奖 2=二等奖 3=三等奖 4=优秀奖',
    reward_name VARCHAR(100) NOT NULL COMMENT '奖项名称',
    prize_name VARCHAR(200) DEFAULT NULL COMMENT '奖品名称',
    prize_image VARCHAR(500) DEFAULT NULL COMMENT '奖品图片URL',
    quota INT NOT NULL DEFAULT 1 COMMENT '名额',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_batch_id (batch_id),
    KEY idx_reward_level (reward_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖项配置';

-- ============================================================
-- 11. 奖品发放记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS reward_issue (
    id BIGINT NOT NULL COMMENT '发放记录ID',
    reward_id BIGINT NOT NULL COMMENT '奖项配置ID',
    work_id BIGINT NOT NULL COMMENT '作品ID',
    issue_status TINYINT NOT NULL DEFAULT 0 COMMENT '发放状态 0=未发放 1=已发放',
    issue_time DATETIME DEFAULT NULL COMMENT '发放时间',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_reward_id (reward_id),
    KEY idx_work_id (work_id),
    KEY idx_issue_status (issue_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖品发放记录';

-- ============================================================
-- 12. 奖品配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS reward_config (
    id BIGINT NOT NULL COMMENT '配置ID',
    batch_id BIGINT NOT NULL COMMENT '评分批次ID',
    reward_level VARCHAR(20) NOT NULL COMMENT '等级（一等奖/二等奖/三等奖/优秀奖）',
    reward_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '奖项名称',
    prize_name VARCHAR(200) NOT NULL DEFAULT '' COMMENT '奖品描述',
    quota INT NOT NULL DEFAULT 1 COMMENT '名额',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_batch_id (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖品配置';

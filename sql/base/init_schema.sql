-- ============================================================
-- 学院作品展示平台 - 基础表结构
-- 版本: 1.0
-- 说明: 仅包含用户、角色、菜单、字典等基础设施表
--       业务表（作品、评分等）由后端2维护
-- ============================================================

CREATE DATABASE IF NOT EXISTS jingxuan DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE jingxuan;

-- ============================================================
-- 1. 系统角色表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码，如 ROLE_STUDENT / ROLE_TEACHER / ROLE_ADMIN',
    description VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

-- ============================================================
-- 2. 系统用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名（学号/工号）',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    role_id INT NOT NULL COMMENT '角色ID',
    class_id BIGINT DEFAULT NULL COMMENT '班级ID（关联sys_dict）',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态 0=禁用 1=启用',
    first_login TINYINT NOT NULL DEFAULT 1 COMMENT '是否首次登录 0=否 1=是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_role_id (role_id),
    KEY idx_class_id (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- ============================================================
-- 3. 系统菜单/权限表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT NOT NULL COMMENT '菜单ID',
    menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID, 0=根菜单',
    path VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
    permission VARCHAR(200) DEFAULT NULL COMMENT '权限标识，如 user:list',
    type TINYINT NOT NULL DEFAULT 1 COMMENT '类型 0=目录 1=菜单 2=按钮',
    icon VARCHAR(100) DEFAULT NULL COMMENT '图标',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单/权限';

-- ============================================================
-- 4. 角色-菜单关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT NOT NULL COMMENT '关联ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关联';

-- ============================================================
-- 5. 数据字典表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT NOT NULL COMMENT '字典ID',
    dict_type VARCHAR(50) NOT NULL COMMENT '字典类型编码，如 class, tech_stack, work_type',
    dict_label VARCHAR(100) NOT NULL COMMENT '字典标签（显示值）',
    dict_value VARCHAR(100) NOT NULL COMMENT '字典键值',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典';

-- ============================================================
-- 6. 操作日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_log (
    id BIGINT NOT NULL COMMENT '日志ID',
    user_id BIGINT DEFAULT NULL COMMENT '操作用户ID',
    username VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
    action VARCHAR(100) NOT NULL COMMENT '操作类型，如 LOGIN / SUBMIT / AUDIT / SCORE',
    target VARCHAR(255) DEFAULT NULL COMMENT '操作目标描述',
    target_id BIGINT DEFAULT NULL COMMENT '操作目标ID',
    ip VARCHAR(50) DEFAULT NULL COMMENT '请求IP',
    request_method VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
    request_path VARCHAR(255) DEFAULT NULL COMMENT '请求路径',
    params TEXT DEFAULT NULL COMMENT '请求参数（JSON）',
    result TINYINT NOT NULL DEFAULT 1 COMMENT '操作结果 0=失败 1=成功',
    error_msg VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    duration BIGINT DEFAULT NULL COMMENT '执行耗时(ms)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_action (action),
    KEY idx_target_id (target_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ============================================================
-- 7. 系统公告表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_notice (
    id BIGINT NOT NULL COMMENT '公告ID',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '公告内容',
    publisher_id BIGINT NOT NULL COMMENT '发布人ID',
    publish_time DATETIME DEFAULT NULL COMMENT '发布时间',
    top_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶 0=否 1=是',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0=草稿 1=已发布',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_publisher_id (publisher_id),
    KEY idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告';

-- ============================================================
-- 8. 消息通知表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_notification (
    id BIGINT NOT NULL COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT DEFAULT NULL COMMENT '通知内容',
    type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '通知类型，如 WORK_AUDIT / WORK_PUBLISH / SCORE / SYSTEM',
    ref_id BIGINT DEFAULT NULL COMMENT '关联业务ID（如作品ID）',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读 0=未读 1=已读',
    read_time DATETIME DEFAULT NULL COMMENT '读取时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_is_read (is_read),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知';

-- ============================================================
-- 初始化数据 - 角色
-- ============================================================
INSERT INTO sys_role (id, role_name, role_code, description) VALUES
(1, '学生', 'ROLE_STUDENT', '学生用户，可提交和管理作品'),
(2, '教师', 'ROLE_TEACHER', '教师用户，可评分和查看作品'),
(3, '管理员', 'ROLE_ADMIN', '系统管理员，可管理所有模块')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- ============================================================
-- 初始化数据 - 管理员账号（密码: admin123，BCrypt加密）
-- ============================================================
INSERT INTO sys_user (id, username, password, real_name, role_id, status, first_login) VALUES
(1, 'admin', '$2a$10$3.ZREK5eola8YSyYibukVu77aiCx0oUwt3nMLW3oIVbE2GZD1lSd.', '系统管理员', 3, 1, 0)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name);

-- ============================================================
-- 初始化数据 - 基础菜单
-- ============================================================
INSERT INTO sys_menu (id, menu_name, parent_id, path, permission, type, icon, sort) VALUES
(1, '系统管理', 0, '/system', NULL, 0, 'Setting', 1),
(2, '用户管理', 1, '/system/users', 'user:list', 1, 'User', 1),
(3, '角色管理', 1, '/system/roles', 'role:list', 1, 'Avatar', 2),
(4, '菜单管理', 1, '/system/menus', 'menu:list', 1, 'Menu', 3),
(5, '字典管理', 1, '/system/dict', 'dict:list', 1, 'Book', 4),
(6, '日志管理', 1, '/system/logs', 'log:list', 1, 'Document', 5),
(7, '公告管理', 0, '/notice', 'notice:list', 1, 'Bell', 2),
(8, '审核管理', 0, '/audit', 'audit:list', 1, 'Checklist', 3),
(9, '评分批次管理', 0, '/score-batch', 'score-batch:list', 1, 'Timer', 4),
(10, '作品管理', 0, '/works', 'work:list', 1, 'FolderOpened', 5),
(11, '内容审核规则', 0, '/sensitive-rule', 'sensitive-rule:list', 1, 'Shield', 6),
(12, '端口管理', 0, '/port', 'port:list', 1, 'Connection', 7),
(13, '创建用户', 2, NULL, 'user:create', 2, NULL, 1),
(14, '编辑用户', 2, NULL, 'user:edit', 2, NULL, 2),
(15, '删除用户', 2, NULL, 'user:delete', 2, NULL, 3),
(16, '审核通过', 8, NULL, 'audit:approve', 2, NULL, 1),
(17, '审核驳回', 8, NULL, 'audit:reject', 2, NULL, 2),
(18, '发布作品', 10, NULL, 'work:publish', 2, NULL, 1),
(19, '下线作品', 10, NULL, 'work:offline', 2, NULL, 2)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- ============================================================
-- 初始化数据 - 管理员角色绑定菜单 (role_id=3)
-- ============================================================
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT
    ROW_NUMBER() OVER (ORDER BY m.id) + 100,
    3,
    m.id
FROM sys_menu m
WHERE m.deleted = 0
ON DUPLICATE KEY UPDATE role_id = 3;

-- 测试种子数据（H2 兼容）

-- ============================================================
-- 角色
-- ============================================================
INSERT INTO sys_role (id, role_name, role_code) VALUES
(1, '学生', 'ROLE_STUDENT'),
(2, '教师', 'ROLE_TEACHER'),
(3, '管理员', 'ROLE_ADMIN');

-- ============================================================
-- 数据字典
-- ============================================================
INSERT INTO sys_dict (id, dict_type, dict_label, dict_value, sort) VALUES
(1, 'class', '2022级软件技术1班', '2022_soft_1', 1),
(2, 'class', '2022级软件技术2班', '2022_soft_2', 2),
(3, 'class', '2022级大数据1班', '2022_bigdata_1', 3),
(10, 'tech_stack', 'Java/Spring Boot', 'Java/Spring Boot', 1),
(11, 'tech_stack', 'Vue.js/Element Plus', 'Vue.js/Element Plus', 2);

-- ============================================================
-- 用户（密码统一为 test123 的 BCrypt）
-- ============================================================
INSERT INTO sys_user (id, username, password, real_name, role_id, class_id, status, first_login) VALUES
-- 学生
(100, '2022001', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '张三', 1, 1, 1, 0),
(101, '2022002', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '李四', 1, 1, 1, 0),
(102, '2022003', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '王五', 1, 2, 1, 0),
-- 教师
(200, 't001', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '张教授', 2, NULL, 1, 0),
(201, 't002', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '李教授', 2, NULL, 1, 0),
-- 管理员
(300, 'admin', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '管理员', 3, NULL, 1, 0);

-- ============================================================
-- 评分批次
-- ============================================================
INSERT INTO score_batch (id, batch_name, start_time, end_time, class_scopes, status) VALUES
(1, '2026年春学期实训评分', '2026-01-01 00:00:00', '2026-07-31 23:59:59', '["2022_soft_1","2022_soft_2"]', 1);

-- ============================================================
-- 作品
-- ============================================================
INSERT INTO work (id, title, summary, tech_stack, advisor, status, submitter_id, submit_time, batch_id, view_count, like_count) VALUES
(1, '校园二手书交易平台', '基于Spring Boot的二手书交易平台', 'Java/Spring Boot', '张教授', 3, 100, '2026-03-15 10:00:00', 1, 10, 3),
(2, '智能垃圾分类系统', '基于深度学习的垃圾分类识别系统', 'Python/Django', '李教授', 3, 102, '2026-03-20 14:00:00', 1, 5, 1),
(3, '学生考勤管理系统', '基于微信小程序的学生考勤系统', '微信小程序', '张教授', 3, 104, '2026-04-01 09:00:00', 1, 0, 0),
(4, '未发布作品', '测试用未发布作品', 'Java/Spring Boot', '王教授', 3, 101, '2026-04-10 11:00:00', 1, 0, 0),
(5, '待审核作品', '测试用待审核作品', 'Python/Flask', '张教授', 1, 103, '2026-05-01 16:00:00', 1, 0, 0),
(6, '草稿作品', '测试用草稿作品', 'Java/Spring Boot', '李教授', 0, 100, NULL, NULL, 0, 0),
(7, '已驳回作品', '测试用已驳回作品', '微信小程序', '王教授', 2, 105, '2026-04-20 08:00:00', 1, 0, 0);

-- ============================================================
-- 团队成员
-- ============================================================
INSERT INTO work_member (id, work_id, student_id, student_name, student_no, class_name, is_leader) VALUES
(1, 1, 100, '张三', '2022001', '2022级软件技术1班', 1),
(2, 1, 101, '李四', '2022002', '2022级软件技术1班', 0);

-- ============================================================
-- 发布状态
-- ============================================================
INSERT INTO work_publish (id, work_id, publish_status, featured, publish_time, publisher_id) VALUES
(1, 1, 1, 1, '2026-03-20 10:00:00', 300),
(2, 2, 1, 0, '2026-03-25 10:00:00', 300),
(3, 3, 1, 0, '2026-04-05 10:00:00', 300);

-- ============================================================
-- 评分
-- ============================================================
INSERT INTO work_score (id, work_id, scorer_id, score, innovation_score, tech_score, completion_score, practicality_score, comment) VALUES
(1, 1, 200, 85.00, 20.00, 22.00, 26.00, 17.00, '优秀作品');

-- ============================================================
-- 标签
-- ============================================================
INSERT INTO tag (id, name, color, type, sort) VALUES
(1, '前端', '#409EFF', 'tech', 1),
(2, '后端', '#67C23A', 'tech', 2),
(3, '人工智能', '#E6A23C', 'tech', 3);

INSERT INTO work_tag (id, work_id, tag_id) VALUES
(1, 1, 1), (2, 1, 2);

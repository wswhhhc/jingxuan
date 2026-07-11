-- ============================================================
-- 集成测试数据（MySQL 8）
-- ============================================================

-- 角色
INSERT INTO sys_role (id, role_name, role_code, description) VALUES
(1, '学生', 'ROLE_STUDENT', '学生用户'),
(2, '教师', 'ROLE_TEACHER', '教师用户'),
(3, '管理员', 'ROLE_ADMIN', '系统管理员');

-- 管理员
INSERT INTO sys_user (id, username, password, real_name, role_id, status, first_login) VALUES
(1, 'admin', '$2a$10$3.ZREK5eola8YSyYibukVu77aiCx0oUwt3nMLW3oIVbE2GZD1lSd.', '系统管理员', 3, 1, 0);

-- 学生
INSERT INTO sys_user (id, username, password, real_name, role_id, class_id, status, first_login) VALUES
(100, '2022001', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '张三', 1, 1, 1, 1),
(101, '2022002', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '李四', 1, 1, 1, 1),
(102, '2022003', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '王五', 1, 2, 1, 1),
(103, '2022004', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '赵六', 1, 2, 1, 1),
(104, '2022005', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '孙七', 1, 3, 1, 1),
(105, '2022006', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '周八', 1, 3, 1, 1);

-- 独立测试学生（无关联作品，用于 CRUD 测试）
INSERT INTO sys_user (id, username, password, real_name, role_id, class_id, status, first_login) VALUES
(110, 'teststu', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '测试学生', 1, 1, 1, 1);

-- 教师
INSERT INTO sys_user (id, username, password, real_name, role_id, status, first_login) VALUES
(200, 't001', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '张教授', 2, 1, 1),
(201, 't002', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '李教授', 2, 1, 1),
(202, 't003', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '王教授', 2, 1, 1);

-- 班级字典
INSERT INTO sys_dict (id, dict_type, dict_label, dict_value, sort) VALUES
(1, 'class', '2022级软件技术1班', '2022_soft_1', 1),
(2, 'class', '2022级软件技术2班', '2022_soft_2', 2),
(3, 'class', '2022级大数据1班', '2022_bigdata_1', 3),
(4, 'class', '2022级人工智能1班', '2022_ai_1', 4),
(10, 'tech_stack', 'Java/Spring Boot', 'Java/Spring Boot', 1),
(11, 'tech_stack', 'Vue.js/Element Plus', 'Vue.js/Element Plus', 2),
(12, 'tech_stack', 'Python/Django', 'Python/Django', 3);

-- 评分批次
INSERT INTO score_batch (id, batch_name, start_time, end_time, class_scopes, status, rank_published) VALUES
(1, '当前实训评分批次', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 365 DAY), '["2022_soft_1","2022_soft_2"]', 1, 1),
(2, '已结束实训评分批次', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 365 DAY), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY), '["2022_soft_1"]', 2, 0);

-- 作品
INSERT INTO work (id, title, summary, tech_stack, advisor, status, submitter_id, submit_time, batch_id, view_count, like_count) VALUES
(1, '校园二手书交易平台', '基于Spring Boot + Vue.js的交易平台', 'Java/Spring Boot,Vue.js/Element Plus', '张教授', 3, 100, '2025-03-15 10:00:00', 1, 42, 5),
(2, '智能垃圾分类识别系统', '基于深度学习的垃圾分类识别', 'Python/Django', '李教授', 3, 102, '2025-03-20 14:00:00', 1, 30, 3),
(3, '学生考勤管理系统', '基于微信小程序的学生考勤系统', '微信小程序,Java/Spring Boot', '张教授', 3, 104, '2025-04-01 09:00:00', 1, 55, 8),
(4, '校园社团管理平台', '基于React + Spring Boot的社团管理', 'React/Next.js,Java/Spring Boot', '王教授', 3, 101, '2025-04-10 11:00:00', 1, 0, 0),
(5, '个人博客系统', '基于Flask的个人博客系统', 'Python/Flask', '张教授', 1, 103, '2025-05-01 16:00:00', 1, 0, 0),
(6, '在线考试系统', '基于Spring Boot的在线考试系统', 'Java/Spring Boot', '李教授', 0, 100, NULL, NULL, 0, 0),
(7, '天气查询小程序', '微信小程序实现的天气查询应用', '微信小程序', '王教授', 2, 105, '2025-04-20 08:00:00', 1, 0, 0);

-- 发布
INSERT INTO work_publish (id, work_id, publish_status, featured, publish_time, publisher_id) VALUES
(1, 1, 1, 0, '2025-03-20 10:00:00', 1),
(2, 2, 1, 0, '2025-03-25 10:00:00', 1),
(3, 3, 1, 1, '2025-04-05 10:00:00', 1),
(4, 4, 0, 0, NULL, NULL);

-- 成员
INSERT INTO work_member (id, work_id, student_id, student_name, student_no, class_name, is_leader) VALUES
(1, 1, 100, '张三', '2022001', '2022级软件技术1班', 1),
(2, 1, 101, '李四', '2022002', '2022级软件技术1班', 0),
(3, 2, 102, '王五', '2022003', '2022级软件技术2班', 1),
(4, 3, 104, '孙七', '2022005', '2022级人工智能1班', 1),
(5, 4, 101, '李四', '2022002', '2022级软件技术1班', 1),
(6, 5, 103, '赵六', '2022004', '2022级软件技术2班', 1),
(7, 6, 100, '张三', '2022001', '2022级软件技术1班', 1),
(8, 7, 105, '周八', '2022006', '2022级大数据1班', 1);

-- 附件
INSERT INTO work_attachment (id, work_id, file_name, file_type, file_size, file_url, category) VALUES
(1, 1, 'occupied.zip', 'zip', 1024, '/uploads/test/occupied.zip', 'attachment');

-- 审核记录
INSERT INTO work_audit (id, work_id, auditor_id, result, reason, audit_time) VALUES
(1, 1, 1, 1, '作品完整，同意发布', '2025-03-18 10:00:00'),
(2, 2, 1, 1, '技术方案可行', '2025-03-22 14:00:00'),
(3, 3, 1, 1, '功能完整', '2025-04-03 09:00:00'),
(4, 4, 1, 1, '作品优秀', '2025-04-12 11:00:00'),
(5, 7, 1, 0, '内容过于简单', '2025-04-22 08:00:00');

-- 评分
INSERT INTO work_score (id, work_id, teacher_id, innovation, difficulty, completion, practicality, total, comment, batch_id) VALUES
(1, 1, 200, 22.00, 20.00, 28.00, 18.00, 88.00, '功能完整，界面美观', 1),
(2, 1, 201, 20.00, 22.00, 26.00, 17.00, 85.00, '技术实现扎实', 1),
(3, 1, 202, 23.00, 21.00, 27.00, 19.00, 90.00, '作品完成度高', 1),
(4, 2, 200, 24.00, 23.00, 25.00, 16.00, 88.00, '技术难度较高', 1),
(5, 2, 201, 22.00, 24.00, 24.00, 15.00, 85.00, '识别准确率有待提高', 1),
(6, 3, 200, 18.00, 16.00, 26.00, 17.00, 77.00, '功能基本完善', 1),
(7, 3, 201, 17.00, 15.00, 25.00, 16.00, 73.00, '建议增加离线模式', 1);

-- 通知
INSERT INTO sys_notification (id, user_id, title, content, type, ref_id, is_read) VALUES
(1, 100, '作品审核通过', '您提交的作品「校园二手书交易平台」已通过审核', 'WORK_AUDIT', 1, 0),
(2, 200, '新评分任务', '有新的作品待评分', 'SCORE', NULL, 0);

-- 标签
INSERT INTO tag (id, name, color, type, sort) VALUES
(1, 'Java', '#F8981D', 'tech', 1),
(2, 'Spring Boot', '#6DB33F', 'tech', 2),
(3, 'Vue', '#4FC08D', 'tech', 3),
(4, '微信小程序', '#07C160', 'tech', 4);

-- 作品-标签关联
INSERT INTO work_tag (id, work_id, tag_id) VALUES
(1, 1, 1), (2, 1, 2), (3, 1, 3),
(4, 3, 4);

-- 奖项
INSERT INTO reward_config (id, batch_id, reward_level, reward_name, prize_name, quota) VALUES
(1, 1, '一等奖', '一等奖', '荣誉证书+500元京东卡', 1),
(2, 1, '二等奖', '二等奖', '荣誉证书+300元京东卡', 2),
(3, 1, '三等奖', '三等奖', '荣誉证书+100元京东卡', 3);

-- 排名奖励
INSERT INTO rank_reward (id, batch_id, reward_level, reward_name, prize_name, quota) VALUES
(1, 1, 1, '第1名', '荣誉证书+500元京东卡', 1),
(2, 1, 2, '第2名', '荣誉证书+300元京东卡', 1),
(3, 1, 3, '第3名', '荣誉证书+100元京东卡', 1);

-- 菜单
INSERT INTO sys_menu (id, menu_name, parent_id, path, permission, type, icon, sort) VALUES
(1, '系统管理', 0, '/system', NULL, 0, 'Setting', 1),
(2, '用户管理', 1, '/system/users', 'user:list', 1, 'User', 1),
(8, '审核管理', 0, '/audit', 'audit:list', 1, 'Checklist', 3);

-- 管理员角色菜单
INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(1, 3, 1), (2, 3, 2), (3, 3, 8);

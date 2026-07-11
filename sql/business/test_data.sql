-- ============================================================
-- 学院作品展示平台 - 测试数据
-- 请先执行 work_schema.sql 建表
-- ============================================================

USE jingxuan;

-- ============================================================
-- 数据字典 - 班级 & 技术栈
-- ============================================================
INSERT INTO sys_dict (id, dict_type, dict_label, dict_value, sort, remark) VALUES
(1, 'class', '2022级软件技术1班', '2022_soft_1', 1, NULL),
(2, 'class', '2022级软件技术2班', '2022_soft_2', 2, NULL),
(3, 'class', '2022级大数据1班', '2022_bigdata_1', 3, NULL),
(4, 'class', '2022级人工智能1班', '2022_ai_1', 4, NULL),
(5, 'class', '2022级计应1班', '2022_ce_1', 5, NULL),
(10, 'tech_stack', 'Java/Spring Boot', 'Java/Spring Boot', 1, NULL),
(11, 'tech_stack', 'Vue.js/Element Plus', 'Vue.js/Element Plus', 2, NULL),
(12, 'tech_stack', 'Python/Django', 'Python/Django', 3, NULL),
(13, 'tech_stack', 'Python/Flask', 'Python/Flask', 4, NULL),
(14, 'tech_stack', 'Go/Gin', 'Go/Gin', 5, NULL),
(15, 'tech_stack', 'Node.js/Express', 'Node.js/Express', 6, NULL),
(16, 'tech_stack', 'React/Next.js', 'React/Next.js', 7, NULL),
(17, 'tech_stack', '微信小程序', '微信小程序', 8, NULL),
(18, 'tech_stack', 'Android/Java', 'Android/Java', 9, NULL),
(19, 'tech_stack', 'Flutter/Dart', 'Flutter/Dart', 10, NULL)
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

-- ============================================================
-- 用户 - 测试学生和教师账号
-- 密码统一为 test123（BCrypt加密）
-- ============================================================
INSERT INTO sys_user (id, username, password, real_name, role_id, class_id, status, first_login) VALUES
-- 学生 (role_id=1)
(100, '2022001', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '张三', 1, 1, 1, 1),
(101, '2022002', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '李四', 1, 1, 1, 1),
(102, '2022003', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '王五', 1, 2, 1, 1),
(103, '2022004', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '赵六', 1, 2, 1, 1),
(104, '2022005', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '孙七', 1, 3, 1, 1),
(105, '2022006', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '周八', 1, 3, 1, 1),
-- 教师 (role_id=2)
(200, 't001', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '张教授', 2, NULL, 1, 1),
(201, 't002', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '李教授', 2, NULL, 1, 1),
(202, 't003', '$2a$10$lK3UmVNy8shFEMT9dNxmJ.YMgPv3U/RFf.r1PysWnUTPVaN.vUTCW', '王教授', 2, NULL, 1, 1)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name);

-- ============================================================
-- 评分批次
-- ============================================================
INSERT INTO score_batch (id, batch_name, start_time, end_time, class_scopes, status) VALUES
(1, '2026年春学期实训评分', '2026-01-01 00:00:00', '2026-07-31 23:59:59', '["2022_soft_1","2022_soft_2","2022_bigdata_1","2022_ai_1","2022_ce_1"]', 1),
(2, '2025年秋学期实训评分', '2025-09-01 00:00:00', '2026-01-31 23:59:59', '["2022_soft_1","2022_soft_2"]', 2)
ON DUPLICATE KEY UPDATE batch_name = VALUES(batch_name);

-- ============================================================
-- 作品数据
-- ============================================================
INSERT INTO work (id, title, summary, tech_stack, advisor, cover_url, status, submitter_id, submit_time, batch_id) VALUES
-- 已通过/已发布作品
(1, '校园二手书交易平台', '基于Spring Boot + Vue.js的校园二手书交易平台，支持图书发布、搜索、在线交流等功能', 'Java/Spring Boot,Vue.js/Element Plus', '张教授', NULL, 3, 100, '2025-03-15 10:00:00', 1),
(2, '智能垃圾分类识别系统', '基于深度学习的垃圾分类识别系统，使用卷积神经网络对常见垃圾进行分类识别', 'Python/Django', '李教授', NULL, 3, 102, '2025-03-20 14:00:00', 1),
(3, '学生考勤管理系统', '基于微信小程序的学生考勤系统，支持GPS定位签到、请假申请、考勤统计等功能', '微信小程序,Java/Spring Boot', '张教授', NULL, 3, 104, '2025-04-01 09:00:00', 1),
-- 已通过/未发布作品
(4, '校园社团管理平台', '基于React + Spring Boot的校园社团管理平台，支持社团创建、活动发布、成员管理', 'React/Next.js,Java/Spring Boot', '王教授', NULL, 3, 101, '2025-04-10 11:00:00', 1),
-- 待审核作品
(5, '个人博客系统', '基于Flask的个人博客系统，支持Markdown编辑、标签分类、评论互动', 'Python/Flask', '张教授', NULL, 1, 103, '2025-05-01 16:00:00', 1),
-- 草稿作品
(6, '在线考试系统', '基于Spring Boot的在线考试系统，支持自动组卷、在线答题、成绩分析', 'Java/Spring Boot', '李教授', NULL, 0, 100, NULL, NULL),
-- 已驳回作品
(7, '天气查询小程序', '微信小程序实现的天气查询应用', '微信小程序', '王教授', NULL, 2, 105, '2025-04-20 08:00:00', 1)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- ============================================================
-- 作品发布状态
-- ============================================================
INSERT INTO work_publish (id, work_id, publish_status, featured, publish_time, publisher_id) VALUES
(1, 1, 1, 0, '2025-03-20 10:00:00', 1),
(2, 2, 1, 0, '2025-03-25 10:00:00', 1),
(3, 3, 1, 1, '2025-04-05 10:00:00', 1),
(4, 4, 0, 0, NULL, NULL)
ON DUPLICATE KEY UPDATE work_id = VALUES(work_id);

-- ============================================================
-- 作品成员
-- ============================================================
INSERT INTO work_member (id, work_id, student_id, student_name, student_no, class_name, is_leader) VALUES
-- 作品1：张三（队长）+ 李四（成员）
(1, 1, 100, '张三', '2022001', '2022级软件技术1班', 1),
(2, 1, 101, '李四', '2022002', '2022级软件技术1班', 0),
-- 作品2：王五（队长）
(3, 2, 102, '王五', '2022003', '2022级大数据1班', 1),
-- 作品3：孙七（队长）
(4, 3, 104, '孙七', '2022005', '2022级人工智能1班', 1),
-- 作品4：李四（队长）
(5, 4, 101, '李四', '2022002', '2022级软件技术1班', 1),
-- 作品5：赵六（队长）
(6, 5, 103, '赵六', '2022004', '2022级软件技术2班', 1),
-- 作品6：张三（队长）
(7, 6, 100, '张三', '2022001', '2022级软件技术1班', 1),
-- 作品7：周八（队长）
(8, 7, 105, '周八', '2022006', '2022级大数据1班', 1)
ON DUPLICATE KEY UPDATE student_name = VALUES(student_name);

-- ============================================================
-- 审核记录
-- ============================================================
INSERT INTO work_audit (id, work_id, auditor_id, result, reason, audit_time) VALUES
(1, 1, 1, 1, '作品完整，同意发布', '2025-03-18 10:00:00'),
(2, 2, 1, 1, '技术方案可行，同意发布', '2025-03-22 14:00:00'),
(3, 3, 1, 1, '功能完整，同意发布', '2025-04-03 09:00:00'),
(4, 4, 1, 1, '作品优秀，同意通过', '2025-04-12 11:00:00'),
(5, 7, 1, 0, '作品内容过于简单，请补充详细信息', '2025-04-22 08:00:00')
ON DUPLICATE KEY UPDATE id = VALUES(id);

-- ============================================================
-- 评分数据（作品1、2、3已被三位教师评分）
-- ============================================================
INSERT INTO work_score (id, work_id, teacher_id, innovation, difficulty, completion, practicality, total, comment, batch_id) VALUES
-- 作品1 - 张教授评分
(1, 1, 200, 22.00, 20.00, 28.00, 18.00, 88.00, '功能完整，界面美观，创新性较好', 1),
-- 作品1 - 李教授评分
(2, 1, 201, 20.00, 22.00, 26.00, 17.00, 85.00, '技术实现扎实，建议补充更多测试用例', 1),
-- 作品1 - 王教授评分
(3, 1, 202, 23.00, 21.00, 27.00, 19.00, 90.00, '作品完成度高，实用性强', 1),
-- 作品2 - 张教授评分
(4, 2, 200, 24.00, 23.00, 25.00, 16.00, 88.00, '技术难度较高，模型训练充分', 1),
-- 作品2 - 李教授评分
(5, 2, 201, 22.00, 24.00, 24.00, 15.00, 85.00, '识别准确率有待提高', 1),
-- 作品3 - 张教授评分
(6, 3, 200, 18.00, 16.00, 26.00, 17.00, 77.00, '功能基本完善，界面有待优化', 1),
-- 作品3 - 李教授评分
(7, 3, 201, 17.00, 15.00, 25.00, 16.00, 73.00, '定位签到功能可用，建议增加离线模式', 1)
ON DUPLICATE KEY UPDATE id = VALUES(id);

-- ============================================================
-- 奖项配置
-- ============================================================
INSERT INTO rank_reward (id, batch_id, reward_level, reward_name, prize_name, quota) VALUES
(1, 1, 1, '一等奖', '荣誉证书 + 500元京东卡', 1),
(2, 1, 2, '二等奖', '荣誉证书 + 300元京东卡', 2),
(3, 1, 3, '三等奖', '荣誉证书 + 100元京东卡', 3),
(4, 1, 4, '优秀奖', '荣誉证书', 4)
ON DUPLICATE KEY UPDATE reward_name = VALUES(reward_name);

-- ============================================================
-- 公告
-- ============================================================
INSERT INTO sys_notice (id, title, content, publisher_id, publish_time, top_flag, status) VALUES
(1, '关于2025年春学期作品提交的通知', '各位同学：2025年春学期作品提交工作现已开始，请各位同学在规定时间内完成作品提交。提交截止日期为2025年6月30日。', 1, '2025-03-01 08:00:00', 1, 1),
(2, '评分工作启动通知', '各位老师：本学期作品评分工作已启动，请登录系统查看已通过审核的作品并进行评分。评分截止日期为2025年7月15日。', 1, '2025-03-20 08:00:00', 0, 1),
(3, '系统使用指南', '欢迎使用学院作品展示平台！本平台支持作品提交、审核、评分、排行等全流程管理。如有问题请联系管理员。', 1, '2025-03-01 08:00:00', 0, 1)
ON DUPLICATE KEY UPDATE title = VALUES(title);

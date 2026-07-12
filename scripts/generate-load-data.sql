-- ============================================================
-- 菁选 k6 压测数据生成脚本
-- 生成 1 万用户、10 万作品、30 万评分、50 万评论
-- ============================================================

-- 先插入测试账号（用于登录压测）
INSERT INTO sys_user (id, username, password, real_name, role_id, status, create_time, update_time)
VALUES
(1000001, 'loadtest_admin', '$2a$12$dummy1234567890abcdefghijklmnopqrstuvwxyz012345', '压测管理员', 3, 1, NOW(), NOW()),
(1000002, 'loadtest_teacher', '$2a$12$dummy1234567890abcdefghijklmnopqrstuvwxyz012345', '压测教师', 2, 1, NOW(), NOW()),
(1000003, 'loadtest_student', '$2a$12$dummy1234567890abcdefghijklmnopqrstuvwxyz012345', '压测学生', 1, 1, NOW(), NOW());

-- 插入 1 万学生用户（使用存储过程批量生成）
DROP PROCEDURE IF EXISTS generate_users;
DELIMITER $$
CREATE PROCEDURE generate_users()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE batch_size INT DEFAULT 1000;
  WHILE i <= 10000 DO
    INSERT INTO sys_user (id, username, password, real_name, role_id, status, create_time, update_time)
    SELECT
      1000000 + i + t.n,
      CONCAT('stu_', LPAD(i + t.n, 6, '0')),
      '$2a$12$dummy1234567890abcdefghijklmnopqrstuvwxyz012345',
      CONCAT('学生', i + t.n),
      FLOOR(RAND() * 2) + 1,
      NOW(),
      NOW()
    FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
          UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t
    WHERE i + t.n <= 10000;
    SET i = i + batch_size;
  END WHILE;
END$$
DELIMITER ;
CALL generate_users();
DROP PROCEDURE generate_users;

-- 插入评分批次
INSERT INTO score_batch (id, batch_name, batch_type, status, class_scopes, start_time, end_time, rank_published, create_time, update_time)
VALUES
(10001, '2026年上学期作品评比', 'normal', 1, '["2022_soft_1","2022_soft_2","2022_soft_3"]', '2026-03-01 00:00:00', '2026-08-31 23:59:59', 1, NOW(), NOW());

-- 插入 10 万作品（使用存储过程批量生成）
DROP PROCEDURE IF EXISTS generate_works;
DELIMITER $$
CREATE PROCEDURE generate_works()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE batch_size INT DEFAULT 1000;
  WHILE i <= 100000 DO
    INSERT INTO work (id, title, summary, tech_stack, advisor, cover_url, status, submitter_id, batch_id, submit_time, view_count, create_time, update_time)
    SELECT
      1000000 + i + t.n,
      CONCAT('作品标题_', i + t.n),
      CONCAT('这是第', i + t.n, '号作品的简要描述，用于展示作品的核心内容和创新点。'),
      ELT(FLOOR(RAND() * 5) + 1, 'Vue,Spring Boot', 'Python,Flask', 'Java,MyBatis', 'React,Node.js', 'Go,Gin'),
      CONCAT('指导老师_', ELT(FLOOR(RAND() * 20) + 1, '张', '李', '王', '赵', '刘', '陈', '杨', '黄', '周', '吴', '徐', '孙', '马', '胡', '朱', '郭', '何', '罗', '高', '林')),
      CONCAT('/uploads/covers/default_', FLOOR(RAND() * 10) + 1, '.jpg'),
      3,
      1000000 + FLOOR(RAND() * 10000) + 1,
      10001,
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 90) DAY),
      FLOOR(RAND() * 1000),
      NOW(),
      NOW()
    FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
          UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t
    WHERE i + t.n <= 100000;
    SET i = i + batch_size;
  END WHILE;
END$$
DELIMITER ;
CALL generate_works();
DROP PROCEDURE generate_works;

-- 插入 30 万评分（100000 作品 × 3 教师平均）
DROP PROCEDURE IF EXISTS generate_scores;
DELIMITER $$
CREATE PROCEDURE generate_scores()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE batch_size INT DEFAULT 1000;
  WHILE i <= 300000 DO
    INSERT INTO work_score (id, work_id, teacher_id, batch_id, innovation, difficulty, completion, practicality, total, comment, create_time, update_time)
    SELECT
      1000000 + i + t.n,
      1000000 + FLOOR(RAND() * 100000) + 1,
      1000002,
      10001,
      ROUND(RAND() * 25, 2),
      ROUND(RAND() * 25, 2),
      ROUND(RAND() * 30, 2),
      ROUND(RAND() * 20, 2),
      ROUND(RAND() * 100, 2),
      ELT(FLOOR(RAND() * 5) + 1, '优秀作品', '完成度高', '技术扎实', '思路清晰', '实用性强'),
      NOW(),
      NOW()
    FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
          UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t
    WHERE i + t.n <= 300000;
    SET i = i + batch_size;
  END WHILE;
END$$
DELIMITER ;
CALL generate_scores();
DROP PROCEDURE generate_scores;

-- 插入 50 万评论
DROP PROCEDURE IF EXISTS generate_comments;
DELIMITER $$
CREATE PROCEDURE generate_comments()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE batch_size INT DEFAULT 1000;
  WHILE i <= 500000 DO
    INSERT INTO work_comment (id, work_id, user_id, content, create_time, update_time)
    SELECT
      1000000 + i + t.n,
      1000000 + FLOOR(RAND() * 100000) + 1,
      1000000 + FLOOR(RAND() * 10000) + 1,
      CONCAT('评论内容_', i + t.n, '：这是一个用于性能测试的评论内容。'),
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY),
      NOW()
    FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
          UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t
    WHERE i + t.n <= 500000;
    SET i = i + batch_size;
  END WHILE;
END$$
DELIMITER ;
CALL generate_comments();
DROP PROCEDURE generate_comments;

-- 最终统计
SELECT 'sys_user' AS table_name, COUNT(*) AS record_count FROM sys_user
UNION ALL SELECT 'work', COUNT(*) FROM work
UNION ALL SELECT 'work_score', COUNT(*) FROM work_score
UNION ALL SELECT 'work_comment', COUNT(*) FROM work_comment;

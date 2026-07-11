-- 评分批次表增加通知字段
-- 管理员创建批次后可配置通知内容（作品要求、上传材料等），
-- 该通知会发送至批次班级范围内的所有学生（包括后续注册的学生）

ALTER TABLE score_batch
    ADD COLUMN notice_title   VARCHAR(200) DEFAULT NULL COMMENT '批次通知标题（如"2026春学期作品提交要求"）',
    ADD COLUMN notice_content TEXT         DEFAULT NULL COMMENT '批次通知内容（作品要求、上传材料等重要说明）';

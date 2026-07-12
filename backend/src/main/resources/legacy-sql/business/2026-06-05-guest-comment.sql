-- 游客评论支持：user_id 允许为空，新增 guest_name 字段
ALTER TABLE work_comment MODIFY user_id BIGINT NULL COMMENT '评论用户ID（游客时为空）';
ALTER TABLE work_comment ADD COLUMN guest_name VARCHAR(50) DEFAULT NULL COMMENT '游客昵称' AFTER user_id;

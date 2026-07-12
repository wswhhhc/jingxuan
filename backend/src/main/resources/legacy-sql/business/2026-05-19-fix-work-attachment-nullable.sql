-- 2026-05-19
-- 修复 work_attachment 与上传流程语义不一致的问题：
-- 附件允许先上传、后绑定作品，因此 work_id 需要允许为空。

USE jingxuan;

ALTER TABLE work_attachment
    MODIFY work_id BIGINT NULL COMMENT '作品ID（上传时可为空，创建作品后再关联）';

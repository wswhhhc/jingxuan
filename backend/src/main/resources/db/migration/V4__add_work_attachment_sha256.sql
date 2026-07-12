-- 上传内容摘要用于迁移校验、重复文件排查与文件清理审计。
ALTER TABLE work_attachment
    ADD COLUMN sha256 CHAR(64) NULL COMMENT '文件内容 SHA-256 十六进制摘要' AFTER file_size;

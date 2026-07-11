USE jingxuan;

SET @work_preview_url_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'work'
      AND COLUMN_NAME = 'preview_url'
);

SET @add_work_preview_url_sql := IF(
    @work_preview_url_exists = 0,
    'ALTER TABLE work ADD COLUMN preview_url VARCHAR(500) DEFAULT NULL COMMENT ''服务器访问地址'' AFTER video_url',
    'SELECT ''work.preview_url already exists'''
);

PREPARE add_work_preview_url_stmt FROM @add_work_preview_url_sql;
EXECUTE add_work_preview_url_stmt;
DEALLOCATE PREPARE add_work_preview_url_stmt;

UPDATE work w
JOIN work_publish p ON p.work_id = w.id
SET w.preview_url = p.preview_url
WHERE (w.preview_url IS NULL OR w.preview_url = '')
  AND p.preview_url IS NOT NULL
  AND p.preview_url <> '';

DROP TABLE IF EXISTS work_runtime;
DROP TABLE IF EXISTS port_manage;

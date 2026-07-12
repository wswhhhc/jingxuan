-- 2026-06-04
-- 移除"上传源码"概念，更新相关字段注释
-- 用户不再需要上传源代码压缩包，附件体系改为通用文件上传

USE jingxuan;

-- 更新 work_attachment.category 字段注释，移除"压缩包"概念
ALTER TABLE work_attachment
    MODIFY category VARCHAR(20) DEFAULT 'attachment'
    COMMENT '附件分类：attachment=附件 screenshot=截图 video=视频 other=其他';

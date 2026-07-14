package com.jingxuan.constant;

/**
 * 通用业务常量
 */
public interface CommonConstants {

    /** 默认页面大小 */
    int DEFAULT_PAGE_SIZE = 10;

    /** 默认当前页 */
    int DEFAULT_PAGE_NUM = 1;

    /** 最大页面大小 */
    int MAX_PAGE_SIZE = 200;

    /** 根节点父ID */
    Long ROOT_PARENT_ID = 0L;

    /** 精选作品标记值 */
    int FEATURED_YES = 1;

    int FEATURED_NO = 0;

    /** 最大精选作品数 */
    int MAX_FEATURED_COUNT = 2;

    /** 用户名正则：字母开头，4-20位字母数字下划线 */
    String USERNAME_REGEX = "^[a-zA-Z]\\w{3,19}$";

    /** 密码最小长度 */
    int PASSWORD_MIN_LENGTH = 6;

    /** 文件上传：单文件最大字节数 */
    long FILE_MAX_SIZE = 200L * 1024 * 1024;

    long IMAGE_MAX_SIZE = 10L * 1024 * 1024;

    long VIDEO_MAX_SIZE = 1536L * 1024 * 1024;

    /** 文件上传白名单 */
    String[] ALLOWED_EXTENSIONS = {"zip", "rar", "7z", "jpg", "png", "gif", "mp4", "pdf"};

    String[] ALLOWED_IMAGE_EXTENSIONS = {"jpg", "png", "gif"};

    /** 禁止扩展名 */
    String[] FORBIDDEN_EXTENSIONS = {"exe", "sh", "bat", "cmd", "com", "msi", "dll"};
}

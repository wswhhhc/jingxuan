package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_log")
public class SysLog extends BaseEntity {

    private Long userId;
    private String username;
    private String action;
    private String target;
    private Long targetId;
    private String ip;
    private String requestMethod;
    private String requestPath;
    private String params;
    private Integer result;
    private String errorMsg;
    private Long duration;
}

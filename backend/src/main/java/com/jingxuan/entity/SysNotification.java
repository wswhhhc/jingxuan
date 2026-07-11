package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("sys_notification")
public class SysNotification extends BaseEntity {

    private Long userId;
    private String title;
    private String content;
    private String type;
    private Long refId;
    private Integer isRead;
    private LocalDateTime readTime;
}

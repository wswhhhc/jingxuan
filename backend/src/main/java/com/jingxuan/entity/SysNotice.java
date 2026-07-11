package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("sys_notice")
public class SysNotice extends BaseEntity {

    private String title;
    private String content;
    private Long publisherId;
    private LocalDateTime publishTime;
    private Integer topFlag;
    private Integer status;

    /**
     * 通知发送范围：student=仅学生  teacher=仅教师  all=全体
     */
    private String targetScope;

    @TableField(exist = false)
    private String publisherName;
}

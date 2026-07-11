package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("work_publish")
public class WorkPublish extends BaseEntity {

    private Long workId;
    private Integer publishStatus;
    private Integer featured;
    private LocalDateTime publishTime;
    private LocalDateTime offlineTime;
    private Long publisherId;
    private String previewUrl;
}

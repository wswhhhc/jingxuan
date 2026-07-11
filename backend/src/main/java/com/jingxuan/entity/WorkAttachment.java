package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("work_attachment")
public class WorkAttachment extends BaseEntity {

    private Long workId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private String category;
}

package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("work")
public class Work extends BaseEntity {

    private String title;
    private String summary;
    private String techStack;
    private String advisor;
    private String coverUrl;
    private String videoUrl;
    private String previewUrl;
    private String runDesc;
    private Integer status;
    private Long submitterId;
    private LocalDateTime submitTime;
    private Long batchId;
    private Integer viewCount;
    private Integer likeCount;
}

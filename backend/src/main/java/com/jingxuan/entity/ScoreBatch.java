package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("score_batch")
public class ScoreBatch extends BaseEntity {

    private String batchName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String classScopes;
    private Integer status;
    private Integer rankPublished;

    /**
     * 批次通知标题（如"2026春学期作品提交要求"）
     */
    private String noticeTitle;

    /**
     * 批次通知内容（作品要求、上传材料等重要说明）
     */
    private String noticeContent;
}

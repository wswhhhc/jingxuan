package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("reward_issue")
public class RewardIssue extends BaseEntity {

    private Long rewardId;
    private Long workId;
    private Integer issueStatus;
    private LocalDateTime issueTime;
    private Long operatorId;
    private String remark;
}

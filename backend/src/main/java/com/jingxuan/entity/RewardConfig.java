package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("reward_config")
public class RewardConfig extends BaseEntity {

    private Long batchId;
    private String rewardLevel;
    private String rewardName;
    private String prizeName;
    private Integer quota;
}

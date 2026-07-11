package com.jingxuan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jingxuan.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("rank_reward")
public class RankReward extends BaseEntity {

    private Long batchId;
    private Integer rewardLevel;
    private String rewardName;
    private String prizeName;
    private String prizeImage;
    private Integer quota;
}

package com.jingxuan.modules.prize.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "奖品配置VO")
public class PrizeVO {

    private Long id;
    private Long batchId;
    private String batchName;
    private String rewardLevel;
    private String rewardName;
    private String prizeName;
    private Integer quota;
    private LocalDateTime createTime;
}

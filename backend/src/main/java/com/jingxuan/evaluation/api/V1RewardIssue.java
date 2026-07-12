package com.jingxuan.evaluation.api;

import com.jingxuan.entity.RewardIssue;
import io.swagger.v3.oas.annotations.media.Schema;

/** v1 奖品发放记录。 */
@Schema(description = "奖品发放记录")
public record V1RewardIssue(
        String id,
        String rewardId,
        String workId,
        String workTitle,
        Integer issueStatus,
        String issueTime,
        String operatorName,
        String remark
) {
    public static V1RewardIssue from(RewardIssue source) {
        return new V1RewardIssue(
                source.getId().toString(),
                source.getRewardId().toString(),
                source.getWorkId().toString(),
                null,
                source.getIssueStatus(),
                source.getIssueTime() == null ? null : source.getIssueTime().toString(),
                null,
                source.getRemark()
        );
    }
}

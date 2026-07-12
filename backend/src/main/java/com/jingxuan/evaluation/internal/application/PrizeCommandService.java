package com.jingxuan.evaluation.internal.application;

import com.jingxuan.api.V1Ids;
import com.jingxuan.entity.RewardConfig;
import com.jingxuan.entity.RewardIssue;
import com.jingxuan.evaluation.api.V1Prize;
import com.jingxuan.evaluation.api.V1PrizeRequest;
import com.jingxuan.modules.prize.service.PrizeService;
import com.jingxuan.modules.prize.service.RewardIssueService;
import com.jingxuan.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 奖品命令用例。 */
@Service
public class PrizeCommandService {

    private final PrizeService prizeService;
    private final RewardIssueService rewardIssueService;

    public PrizeCommandService(PrizeService prizeService, RewardIssueService rewardIssueService) {
        this.prizeService = prizeService;
        this.rewardIssueService = rewardIssueService;
    }

    /** 创建奖品。 */
    @Transactional
    public V1Prize createPrize(V1PrizeRequest request) {
        RewardConfig config = new RewardConfig();
        config.setBatchId(V1Ids.parse(request.batchId(), "batchId"));
        config.setRewardLevel(request.rewardLevel());
        config.setRewardName(request.rewardName());
        config.setPrizeName(request.prizeName());
        config.setQuota(request.quota());
        Long id = prizeService.createPrize(config);
        return new V1Prize(id.toString(), request.batchId(), null, request.rewardLevel(),
                request.rewardName(), request.prizeName(), request.quota());
    }

    /** 更新奖品。 */
    @Transactional
    public void updatePrize(Long id, V1PrizeRequest request) {
        RewardConfig config = new RewardConfig();
        config.setId(id);
        config.setBatchId(V1Ids.parse(request.batchId(), "batchId"));
        config.setRewardLevel(request.rewardLevel());
        config.setRewardName(request.rewardName());
        config.setPrizeName(request.prizeName());
        config.setQuota(request.quota());
        prizeService.updatePrize(config);
    }

    /** 删除奖品。 */
    @Transactional
    public void deletePrize(Long id) {
        prizeService.deletePrize(id);
    }

    /** 发放奖品。 */
    @Transactional
    public void issuePrize(Long rewardId, Long workId) {
        rewardIssueService.issue(rewardId, workId, SecurityUtils.requireCurrentUserId());
    }

    /** 取消发放。 */
    @Transactional
    public void cancelIssue(Long issueId) {
        rewardIssueService.cancelIssue(issueId);
    }
}

package com.jingxuan.modules.prize.service;

import com.jingxuan.common.PageResult;
import com.jingxuan.modules.prize.dto.PrizeVO;
import com.jingxuan.entity.RewardConfig;

public interface PrizeService {

    PageResult<PrizeVO> queryPrizeList(int pageNum, int pageSize, Long batchId);

    Long createPrize(RewardConfig config);

    void updatePrize(RewardConfig config);

    void deletePrize(Long id);
}

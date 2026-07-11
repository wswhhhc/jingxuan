package com.jingxuan.modules.prize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.RewardConfig;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.mapper.RewardConfigMapper;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.modules.prize.dto.PrizeVO;
import com.jingxuan.modules.prize.service.PrizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrizeServiceImpl implements PrizeService {

    private final RewardConfigMapper rewardConfigMapper;
    private final ScoreBatchMapper scoreBatchMapper;

    @Override
    public PageResult<PrizeVO> queryPrizeList(int pageNum, int pageSize, Long batchId) {
        Page<RewardConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RewardConfig> wrapper = new LambdaQueryWrapper<RewardConfig>()
                .orderByDesc(RewardConfig::getCreateTime);
        if (batchId != null) {
            wrapper.eq(RewardConfig::getBatchId, batchId);
        }
        Page<RewardConfig> result = rewardConfigMapper.selectPage(page, wrapper);

        // 查询批次名称映射
        List<Long> batchIds = result.getRecords().stream()
                .map(RewardConfig::getBatchId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> batchNameMap = batchIds.isEmpty() ? Map.of() :
                scoreBatchMapper.selectBatchIds(batchIds).stream()
                        .collect(Collectors.toMap(ScoreBatch::getId, ScoreBatch::getBatchName));

        List<PrizeVO> voList = result.getRecords().stream().map(r -> {
            PrizeVO vo = new PrizeVO();
            vo.setId(r.getId());
            vo.setBatchId(r.getBatchId());
            vo.setBatchName(batchNameMap.getOrDefault(r.getBatchId(), null));
            vo.setRewardLevel(r.getRewardLevel());
            vo.setRewardName(r.getRewardName());
            vo.setPrizeName(r.getPrizeName());
            vo.setQuota(r.getQuota());
            vo.setCreateTime(r.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPrize(RewardConfig config) {
        rewardConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePrize(RewardConfig config) {
        rewardConfigMapper.updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePrize(Long id) {
        rewardConfigMapper.deleteById(id);
    }
}

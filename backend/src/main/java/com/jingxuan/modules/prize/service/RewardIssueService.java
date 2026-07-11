package com.jingxuan.modules.prize.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.entity.RewardIssue;

import java.util.List;

public interface RewardIssueService {

    Page<RewardIssue> listByPage(int page, int size, Long rewardId);

    void issue(Long rewardId, Long workId, Long operatorId);

    void cancelIssue(Long issueId);

    List<RewardIssue> getByWorkId(Long workId);
}

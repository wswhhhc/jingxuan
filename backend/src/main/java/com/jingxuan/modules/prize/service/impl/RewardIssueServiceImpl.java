package com.jingxuan.modules.prize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.entity.RewardConfig;
import com.jingxuan.entity.RewardIssue;
import com.jingxuan.entity.Work;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.RewardConfigMapper;
import com.jingxuan.mapper.RewardIssueMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.prize.service.RewardIssueService;
import com.jingxuan.modules.work.service.WorkMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardIssueServiceImpl extends ServiceImpl<RewardIssueMapper, RewardIssue> implements RewardIssueService {

    @Override
    public Page<RewardIssue> listByPage(int page, int size, Long rewardId) {
        LambdaQueryWrapper<RewardIssue> qw = new LambdaQueryWrapper<>();
        if (rewardId != null) qw.eq(RewardIssue::getRewardId, rewardId);
        qw.orderByDesc(RewardIssue::getCreateTime);
        return page(new Page<>(page, size), qw);
    }

    private final WorkMapper workMapper;
    private final RewardConfigMapper rewardConfigMapper;
    private final WorkMemberService workMemberService;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issue(Long rewardId, Long workId, Long operatorId) {
        RewardIssue entity = new RewardIssue();
        entity.setRewardId(rewardId);
        entity.setWorkId(workId);
        entity.setIssueStatus(1);
        entity.setIssueTime(LocalDateTime.now());
        entity.setOperatorId(operatorId);
        save(entity);

        // 发送奖励通知给小组成员
        Work work = workMapper.selectById(workId);
        if (work != null) {
            RewardConfig config = rewardConfigMapper.selectById(rewardId);
            String rewardName = config != null ? config.getRewardName() : "获奖";
            String title = "恭喜获奖";
            String message = "您的作品《" + work.getTitle() + "》获得了「" + rewardName + "」";
            List<com.jingxuan.entity.WorkMember> members = workMemberService.getByWorkId(workId);
            for (com.jingxuan.entity.WorkMember member : members) {
                if (member.getStudentId() != null) {
                    notificationService.sendNotification(
                            member.getStudentId(), title, message, "prize", workId);
                }
            }
        }
    }

    @Override
    public void cancelIssue(Long issueId) {
        RewardIssue entity = getById(issueId);
        if (entity == null) throw new BusinessException("发放记录不存在");
        entity.setIssueStatus(0);
        entity.setIssueTime(null);
        updateById(entity);
    }

    @Override
    public List<RewardIssue> getByWorkId(Long workId) {
        return list(new LambdaQueryWrapper<RewardIssue>()
                .eq(RewardIssue::getWorkId, workId));
    }
}

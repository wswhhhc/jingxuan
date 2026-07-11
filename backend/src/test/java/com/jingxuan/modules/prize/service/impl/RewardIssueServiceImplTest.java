package com.jingxuan.modules.prize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.entity.RewardIssue;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.RewardConfigMapper;
import com.jingxuan.mapper.RewardIssueMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.work.service.WorkMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardIssueServiceImpl - 奖品发放服务")
class RewardIssueServiceImplTest {

    @Mock private RewardIssueMapper rewardIssueMapper;
    @Mock private WorkMapper workMapper;
    @Mock private RewardConfigMapper rewardConfigMapper;
    @Mock private WorkMemberService workMemberService;
    @Mock private NotificationService notificationService;

    private RewardIssueServiceImpl rewardIssueService;

    @BeforeEach
    void setUp() {
        rewardIssueService = new RewardIssueServiceImpl(workMapper, rewardConfigMapper, workMemberService, notificationService);
        ReflectionTestUtils.setField(rewardIssueService, "baseMapper", rewardIssueMapper);
    }

    private RewardIssue createIssue(Long id, Long rewardId, Long workId, Integer status) {
        RewardIssue issue = new RewardIssue();
        issue.setId(id);
        issue.setRewardId(rewardId);
        issue.setWorkId(workId);
        issue.setIssueStatus(status);
        return issue;
    }

    @Nested
    @DisplayName("发放奖品")
    class IssuePrize {

        @Test
        @DisplayName("成功发放奖品")
        void shouldIssuePrize() {
            when(rewardIssueMapper.insert(any(RewardIssue.class))).thenReturn(1);

            rewardIssueService.issue(1L, 10L, 100L);

            verify(rewardIssueMapper).insert((RewardIssue) argThat(entity -> {
                assertEquals(1L, ((RewardIssue) entity).getRewardId());
                assertEquals(10L, ((RewardIssue) entity).getWorkId());
                assertEquals(100L, ((RewardIssue) entity).getOperatorId());
                assertEquals(Integer.valueOf(1), ((RewardIssue) entity).getIssueStatus());
                assertNotNull(((RewardIssue) entity).getIssueTime());
                return true;
            }));
        }
    }

    @Nested
    @DisplayName("取消发放")
    class CancelIssue {

        @Test
        @DisplayName("成功取消发放")
        void shouldCancelIssue() {
            RewardIssue existing = createIssue(1L, 1L, 10L, 1);
            when(rewardIssueMapper.selectById(1L)).thenReturn(existing);

            rewardIssueService.cancelIssue(1L);

            verify(rewardIssueMapper).updateById((RewardIssue) argThat(entity -> {
                assertEquals(Integer.valueOf(0), ((RewardIssue) entity).getIssueStatus());
                assertNull(((RewardIssue) entity).getIssueTime());
                return true;
            }));
        }

        @Test
        @DisplayName("发放记录不存在抛异常")
        void shouldThrowWhenNotFound() {
            when(rewardIssueMapper.selectById(999L)).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> rewardIssueService.cancelIssue(999L));
        }
    }

    @Nested
    @DisplayName("查询发放记录")
    class QueryIssue {

        @Test
        @DisplayName("分页查询发放记录")
        void shouldListByPage() {
            Page<RewardIssue> pageResult = new Page<>(1, 10, 1);
            pageResult.setRecords(List.of(createIssue(1L, 1L, 10L, 1)));
            when(rewardIssueMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(pageResult);

            Page<RewardIssue> result = rewardIssueService.listByPage(1, 10, 1L);

            assertEquals(1, result.getTotal());
            assertEquals(1L, result.getRecords().get(0).getRewardId());
        }

        @Test
        @DisplayName("按作品查询发放记录")
        void shouldGetByWorkId() {
            when(rewardIssueMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(createIssue(1L, 1L, 10L, 1)));

            List<RewardIssue> result = rewardIssueService.getByWorkId(10L);

            assertEquals(1, result.size());
            assertEquals(Long.valueOf(10L), result.get(0).getWorkId());
        }
    }
}

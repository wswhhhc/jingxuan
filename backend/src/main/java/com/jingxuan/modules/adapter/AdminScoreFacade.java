package com.jingxuan.modules.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.modules.score.dto.AdminScoreDetailVO;
import com.jingxuan.modules.score.dto.ScoreVO;
import com.jingxuan.modules.score.service.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminScoreFacade {

    private final WorkMapper workMapper;
    private final SysUserMapper sysUserMapper;
    private final ScoreService scoreService;

    public List<AdminScoreDetailVO> getBatchScoreDetail(Long batchId) {
        List<Work> works = workMapper.selectList(
                new LambdaQueryWrapper<Work>()
                        .eq(Work::getBatchId, batchId)
                        .eq(Work::getStatus, 3));
        List<AdminScoreDetailVO> result = new ArrayList<>();
        for (Work work : works) {
            result.add(toScoreDetail(work));
        }
        return result;
    }

    private AdminScoreDetailVO toScoreDetail(Work work) {
        AdminScoreDetailVO detail = new AdminScoreDetailVO();
        detail.setWorkId(work.getId());
        detail.setWorkTitle(work.getTitle());
        SysUser user = sysUserMapper.selectById(work.getSubmitterId());
        detail.setSubmitterName(user != null ? user.getRealName() : null);
        detail.setScores(scoreService.getWorkScores(work.getId()).stream()
                .map(this::toTeacherScoreItem)
                .toList());
        return detail;
    }

    private AdminScoreDetailVO.TeacherScoreItem toTeacherScoreItem(ScoreVO score) {
        AdminScoreDetailVO.TeacherScoreItem item = new AdminScoreDetailVO.TeacherScoreItem();
        item.setTeacherName(score.getTeacherName());
        item.setInnovation(score.getInnovation());
        item.setDifficulty(score.getDifficulty());
        item.setCompletion(score.getCompletion());
        item.setPracticality(score.getPracticality());
        item.setTotal(score.getTotal());
        item.setComment(score.getComment());
        return item;
    }
}

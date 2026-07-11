package com.jingxuan.modules.work.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.entity.WorkMember;
import com.jingxuan.mapper.WorkMemberMapper;
import com.jingxuan.modules.work.dto.WorkMemberDTO;
import com.jingxuan.modules.work.service.WorkMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 作品成员 服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkMemberServiceImpl extends ServiceImpl<WorkMemberMapper, WorkMember> implements WorkMemberService {

    @Override
    public List<WorkMember> getByWorkId(Long workId) {
        return lambdaQuery()
                .eq(WorkMember::getWorkId, workId)
                .orderByDesc(WorkMember::getIsLeader)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMembers(Long workId, List<WorkMemberDTO> members) {
        // 先删除旧成员
        removeByWorkId(workId);

        if (members == null || members.isEmpty()) {
            return;
        }

        List<WorkMember> entityList = members.stream().map(dto -> {
            WorkMember member = new WorkMember();
            member.setWorkId(workId);
            // studentId 可能为空（未注册成员）
            if (dto.getStudentId() != null) {
                member.setStudentId(dto.getStudentId());
            }
            member.setStudentName(dto.getStudentName());
            member.setStudentNo(dto.getStudentNo());
            member.setClassName(dto.getClassName());
            member.setIsLeader(dto.getIsLeader());
            return member;
        }).collect(Collectors.toList());
        saveBatch(entityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByWorkId(Long workId) {
        lambdaUpdate()
                .eq(WorkMember::getWorkId, workId)
                .remove();
    }
}

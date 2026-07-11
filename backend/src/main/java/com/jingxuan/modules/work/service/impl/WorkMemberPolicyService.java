package com.jingxuan.modules.work.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkMember;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkMemberMapper;
import com.jingxuan.modules.work.dto.WorkMemberDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class WorkMemberPolicyService {

    private final SysUserMapper sysUserMapper;
    private final WorkMemberMapper workMemberMapper;
    private final WorkMapper workMapper;

    void resolveMemberStudentIds(List<WorkMemberDTO> members) {
        if (CollectionUtil.isEmpty(members)) {
            return;
        }
        for (WorkMemberDTO member : members) {
            if (member.getStudentId() == null && StrUtil.isNotBlank(member.getStudentNo())) {
                SysUser user = sysUserMapper.findByUsername(member.getStudentNo());
                if (user != null) {
                    member.setStudentId(user.getId());
                }
            }
        }
    }

    void ensureMembersAvailableInBatch(List<WorkMemberDTO> members, Long batchId, Long currentWorkId) {
        if (CollectionUtil.isEmpty(members) || batchId == null) {
            return;
        }
        // 安全查询：先查出该批次下的所有作品 ID（使用 QueryWrapper 避免 Mockito 下 lambda 序列化失败）
        List<Object> batchWorkIds = workMapper.selectObjs(
                Wrappers.<Work>query()
                        .select("id")
                        .eq("batch_id", batchId)
                        .eq("deleted", 0)
                        .ne(currentWorkId != null, "id", currentWorkId));
        if (batchWorkIds.isEmpty()) {
            return;
        }
        for (WorkMemberDTO member : members) {
            if (member.getStudentId() == null) {
                continue;
            }
            Long memberCount = workMemberMapper.selectCount(
                    Wrappers.<WorkMember>query()
                            .eq("student_id", member.getStudentId())
                            .in("work_id", batchWorkIds));
            if (memberCount > 0) {
                throw new BusinessException("团队成员 " + member.getStudentName() + " 在当前批次中已有作品，每个学生只能参与一个作品");
            }
        }
    }
}

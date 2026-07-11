package com.jingxuan.modules.work.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.entity.WorkMember;
import com.jingxuan.modules.work.dto.WorkMemberDTO;

import java.util.List;

public interface WorkMemberService extends IService<WorkMember> {

    /**
     * 根据作品ID获取成员列表
     */
    List<WorkMember> getByWorkId(Long workId);

    /**
     * 批量保存成员（先删除旧成员，再插入新成员）
     */
    void saveMembers(Long workId, List<WorkMemberDTO> members);

    /**
     * 删除作品的所有成员
     */
    void removeByWorkId(Long workId);
}

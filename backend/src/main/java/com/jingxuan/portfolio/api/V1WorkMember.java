package com.jingxuan.portfolio.api;

import com.jingxuan.modules.work.dto.WorkMemberDTO;

/** v1 作品成员。 */
public record V1WorkMember(String id, String studentId, String name, String studentNumber,
                           String className, boolean leader, String avatarUrl) {
}

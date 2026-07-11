package com.jingxuan.portfolio.api;

import com.jingxuan.modules.work.dto.WorkMemberDTO;

/** v1 作品成员。 */
public record V1WorkMember(String id, String studentId, String name, String studentNumber,
                           String className, boolean leader, String avatarUrl) {
    static V1WorkMember from(WorkMemberDTO value) {
        return new V1WorkMember(id(value.getId()), id(value.getStudentId()), value.getStudentName(), value.getStudentNo(),
                value.getClassName(), Integer.valueOf(1).equals(value.getIsLeader()), value.getAvatar());
    }

    static V1WorkMember publicFrom(WorkMemberDTO value) {
        return new V1WorkMember(null, null, value.getStudentName(), null, null,
                Integer.valueOf(1).equals(value.getIsLeader()), value.getAvatar());
    }

    private static String id(Long value) { return value == null ? null : value.toString(); }
}

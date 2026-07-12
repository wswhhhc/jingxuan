package com.jingxuan.portfolio.api;

import com.jingxuan.modules.work.dto.WorkMemberDTO;

/** v1 作品成员。 */
public record V1WorkMember(String id, String studentId, String name, String studentNumber,
                           String className, boolean leader, String avatarUrl) {
    public static V1WorkMember from(WorkMemberDTO dto) {
        return new V1WorkMember(
            id(dto.getId()),
            id(dto.getStudentId()),
            dto.getStudentName(),
            dto.getStudentNo(),
            dto.getClassName(),
            Integer.valueOf(1).equals(dto.getIsLeader()),
            dto.getAvatar()
        );
    }
    private static String id(Long value) { return value == null ? null : value.toString(); }
}

package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.exception.NotFoundException;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.modules.log.service.LogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserApprovalServiceTest {

    private final SysUserMapper users = mock(SysUserMapper.class);
    private final LogService logs = mock(LogService.class);
    private final UserApprovalService service = new UserApprovalService(users, logs);

    @Test
    void approvesOnlyPendingTeacherAndWritesAnAuditAction() {
        SysUser teacher = pendingTeacher();
        when(users.selectById(teacher.getId())).thenReturn(teacher);

        service.decide(teacher.getId(), "APPROVED", null);

        ArgumentCaptor<SysUser> captured = ArgumentCaptor.forClass(SysUser.class);
        verify(users).updateById(captured.capture());
        assertEquals(UserStatusEnum.ENABLED, captured.getValue().getStatus());
        verify(logs).recordAction("USER_APPROVAL_APPROVED", "TEACHER", teacher.getId());
    }

    @Test
    void rejectsPendingTeacherByDisablingTheAccountAndPreservesAuditReason() {
        SysUser teacher = pendingTeacher();
        when(users.selectById(teacher.getId())).thenReturn(teacher);

        service.decide(teacher.getId(), "REJECTED", "材料不完整");

        ArgumentCaptor<SysUser> captured = ArgumentCaptor.forClass(SysUser.class);
        verify(users).updateById(captured.capture());
        assertEquals(UserStatusEnum.DISABLED, captured.getValue().getStatus());
        verify(logs).recordAction("USER_APPROVAL_REJECTED", "TEACHER;reason=材料不完整", teacher.getId());
    }

    @Test
    void refusesNonPendingOrNonTeacherAccountsWithoutWritingAnything() {
        SysUser student = pendingTeacher();
        student.setRoleId(1);
        when(users.selectById(student.getId())).thenReturn(student);

        assertThrows(BusinessException.class, () -> service.decide(student.getId(), "APPROVED", null));

        verifyNoInteractions(logs);
    }

    @Test
    void reportsMissingAccountsAsNotFound() {
        when(users.selectById(9L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> service.decide(9L, "APPROVED", null));

        verifyNoInteractions(logs);
    }

    @Test
    void requiresAReasonWhenRejectingATeacher() {
        SysUser teacher = pendingTeacher();
        when(users.selectById(teacher.getId())).thenReturn(teacher);

        assertThrows(BusinessException.class, () -> service.decide(teacher.getId(), "REJECTED", " "));

        verifyNoInteractions(logs);
    }

    private static SysUser pendingTeacher() {
        SysUser user = new SysUser();
        user.setId(8L);
        user.setRoleId(2);
        user.setStatus(UserStatusEnum.PENDING_APPROVAL);
        return user;
    }
}

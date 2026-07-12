package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.entity.SysUser;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.DeleteRequestMapper;
import com.jingxuan.mapper.StudentTaskMapper;
import com.jingxuan.mapper.SysNotificationMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkMemberMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDeletionServiceTest {

    @Test
    void requiresConfirmationBeforeDeletingReferencedUserData() {
        Fixture fixture = new Fixture();
        when(fixture.users.selectById(9L)).thenReturn(user());
        when(fixture.tasks.selectCount(any())).thenReturn(2L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> fixture.service.delete(9L, false));

        assertEquals(409, exception.getCode());
    }

    @Test
    void deletesSupportedDependentRecordsBeforeTheUserAfterConfirmation() {
        Fixture fixture = new Fixture();
        when(fixture.users.selectById(9L)).thenReturn(user());
        when(fixture.users.physicalDeleteById(9L)).thenReturn(1);

        fixture.service.delete(9L, true);

        verify(fixture.deleteRequests).physicalDeleteByStudentId(9L);
        verify(fixture.tasks).physicalDeleteByUserId(9L);
        verify(fixture.notifications).physicalDeleteByUserId(9L);
        verify(fixture.users).physicalDeleteById(9L);
    }

    @Test
    void rejectsDeletionWhenUserStillOwnsOrBelongsToWorks() {
        Fixture fixture = new Fixture();
        when(fixture.users.selectById(9L)).thenReturn(user());
        when(fixture.works.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> fixture.service.delete(9L, true));

        assertEquals(409, exception.getCode());
    }

    @Test
    void protectsTheRootAdministratorFromPhysicalDeletion() {
        Fixture fixture = new Fixture();
        SysUser root = user();
        root.setUsername("admin");
        root.setRoleId(3);
        when(fixture.users.selectById(9L)).thenReturn(root);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> fixture.service.delete(9L, true));

        assertEquals(403, exception.getCode());
    }

    private static SysUser user() {
        SysUser user = new SysUser();
        user.setId(9L);
        user.setUsername("student-9");
        user.setRoleId(1);
        return user;
    }

    private static class Fixture {
        final SysUserMapper users = mock(SysUserMapper.class);
        final DeleteRequestMapper deleteRequests = mock(DeleteRequestMapper.class);
        final StudentTaskMapper tasks = mock(StudentTaskMapper.class);
        final SysNotificationMapper notifications = mock(SysNotificationMapper.class);
        final WorkMapper works = mock(WorkMapper.class);
        final WorkMemberMapper members = mock(WorkMemberMapper.class);
        final UserDeletionService service = new UserDeletionService(
                users, deleteRequests, tasks, notifications, works, members);
    }
}

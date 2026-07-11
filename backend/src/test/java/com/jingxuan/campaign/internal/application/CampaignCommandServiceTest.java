package com.jingxuan.campaign.internal.application;

import com.jingxuan.entity.StudentTask;
import com.jingxuan.mapper.StudentTaskMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampaignCommandServiceTest {
    private final StudentTaskMapper mapper = mock(StudentTaskMapper.class);
    private final CampaignCommandService service = new CampaignCommandService(mapper);

    @Test
    void completesOnlyOwnersPendingTask() {
        StudentTask task = new StudentTask(); task.setId(8L); task.setUserId(7L); task.setStatus(0);
        when(mapper.selectById(8L)).thenReturn(task);
        service.completeTask(7L, 8L, 99L);
        assertEquals(1, task.getStatus()); assertEquals(99L, task.getWorkId()); verify(mapper).updateById(task);
    }

    @Test
    void rejectsAnotherUsersTask() {
        StudentTask task = new StudentTask(); task.setUserId(8L); task.setStatus(0);
        when(mapper.selectById(1L)).thenReturn(task);
        assertThrows(RuntimeException.class, () -> service.completeTask(7L, 1L, 99L));
    }
}

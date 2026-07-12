package com.jingxuan.workflow;

import com.jingxuan.campaign.api.CampaignTaskCompletion;
import com.jingxuan.modules.work.dto.WorkRequest;
import com.jingxuan.modules.work.service.WorkService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

class TaskWorkSubmissionWorkflowTest {

    @Test
    void createsWorkThenCompletesTheOwnedTaskInTheSameWorkflow() {
        WorkService works = mock(WorkService.class);
        CampaignTaskCompletion tasks = mock(CampaignTaskCompletion.class);
        TaskWorkSubmissionWorkflow workflow = new TaskWorkSubmissionWorkflow(works, tasks);
        WorkRequest request = new WorkRequest();
        when(works.createWork(request)).thenReturn(99L);

        Long workId = workflow.createWorkForTask(7L, 8L, request);

        assertEquals(99L, workId);
        verify(tasks).completeTask(7L, 8L, 99L);
    }

    @Test
    void propagatesTaskCompletionFailureSoTheTransactionCanRollBackWorkCreation() {
        WorkService works = mock(WorkService.class);
        CampaignTaskCompletion tasks = mock(CampaignTaskCompletion.class);
        TaskWorkSubmissionWorkflow workflow = new TaskWorkSubmissionWorkflow(works, tasks);
        WorkRequest request = new WorkRequest();
        when(works.createWork(request)).thenReturn(99L);
        org.mockito.Mockito.doThrow(new IllegalStateException("待办状态冲突"))
                .when(tasks).completeTask(7L, 8L, 99L);

        assertThrows(IllegalStateException.class, () -> workflow.createWorkForTask(7L, 8L, request));
    }

    @Test
    void isDeclaredTransactionalForTheCrossModuleStateChange() throws Exception {
        Transactional transactional = TaskWorkSubmissionWorkflow.class
                .getDeclaredMethod("createWorkForTask", Long.class, Long.class, WorkRequest.class)
                .getAnnotation(Transactional.class);

        org.junit.jupiter.api.Assertions.assertNotNull(transactional);
    }

    @Test
    void submitsWorkBeforeCompletingTheTask() {
        WorkService works = mock(WorkService.class);
        CampaignTaskCompletion tasks = mock(CampaignTaskCompletion.class);
        TaskWorkSubmissionWorkflow workflow = new TaskWorkSubmissionWorkflow(works, tasks);

        workflow.completeTaskForSubmittedWork(7L, 8L, 99L);

        var order = inOrder(works, tasks);
        order.verify(works).submitWork(99L);
        order.verify(tasks).completeTask(7L, 8L, 99L);
    }
}

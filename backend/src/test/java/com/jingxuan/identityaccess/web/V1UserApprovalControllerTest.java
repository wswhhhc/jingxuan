package com.jingxuan.identityaccess.web;

import com.jingxuan.api.V1ExceptionHandler;
import com.jingxuan.identityaccess.internal.application.UserApprovalService;
import com.jingxuan.identityaccess.internal.application.UserDeletionService;
import com.jingxuan.identityaccess.api.V1UserDeletionImpact;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class V1UserApprovalControllerTest {

    @Test
    void delegatesValidatedApprovalDecisionAndRequiresPermissionCode() throws Exception {
        UserApprovalService service = mock(UserApprovalService.class);
        UserDeletionService deletionService = mock(UserDeletionService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new V1UserApprovalController(service, deletionService))
                .setControllerAdvice(new V1ExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/users/9007199254740993/approval-decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECTED\",\"reason\":\"材料不完整\"}"))
                .andExpect(status().isNoContent());

        verify(service).decide(9007199254740993L, "REJECTED", "材料不完整");
        Method method = V1UserApprovalController.class.getDeclaredMethod("decide", String.class,
                com.jingxuan.identityaccess.api.V1UserApprovalDecisionRequest.class);
        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
        assertNotNull(authorization);
        assertEquals("hasAuthority('user:approve')", authorization.value());
    }

    @Test
    void exposesDeletionImpactAndConfirmationThroughUserDeletePermission() throws Exception {
        UserApprovalService approvalService = mock(UserApprovalService.class);
        UserDeletionService deletionService = mock(UserDeletionService.class);
        org.mockito.Mockito.when(deletionService.impact(9007199254740993L)).thenReturn(
                new V1UserDeletionImpact("user", "9007199254740993", 1, List.of("student_task: 1"), false));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new V1UserApprovalController(approvalService, deletionService))
                .setControllerAdvice(new V1ExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/users/9007199254740993/deletion-impact"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/users/9007199254740993/with-confirm").param("confirm", "true"))
                .andExpect(status().isNoContent());

        verify(deletionService).delete(9007199254740993L, true);
        Method method = V1UserApprovalController.class.getDeclaredMethod("deleteWithConfirm", String.class, boolean.class);
        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
        assertNotNull(authorization);
        assertEquals("hasAuthority('user:delete')", authorization.value());
    }
}

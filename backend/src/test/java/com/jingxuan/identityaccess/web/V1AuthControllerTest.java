package com.jingxuan.identityaccess.web;

import com.jingxuan.auth.model.LoginResponse;
import com.jingxuan.auth.model.UserInfoVO;
import com.jingxuan.auth.service.AuthService;
import com.jingxuan.auth.model.LoginRequest;
import com.jingxuan.identityaccess.api.V1LoginResponse;
import com.jingxuan.identityaccess.api.V1UserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class V1AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final V1AuthController controller = new V1AuthController(authService);

    @Test
    void loginUsesOpaqueStringIds() {
        LoginRequest request = new LoginRequest();
        LoginResponse login = LoginResponse.builder()
                .token("access-token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .userInfo(UserInfoVO.builder().id(9007199254740993L).classId(42L).username("student").build())
                .build();
        when(authService.login(request)).thenReturn(login);

        ResponseEntity<V1LoginResponse> response = controller.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("9007199254740993", response.getBody().user().id());
        assertEquals("42", response.getBody().user().classId());
        assertEquals("access-token", response.getBody().accessToken());
    }

    @Test
    void logoutReturnsNoContentAndDelegates() {
        ResponseEntity<Void> response = controller.logout();

        assertEquals(204, response.getStatusCode().value());
        verify(authService).logout();
    }
}

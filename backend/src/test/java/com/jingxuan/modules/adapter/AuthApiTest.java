package com.jingxuan.modules.adapter;

import com.jingxuan.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthApi - 认证接口集成测试")
class AuthApiTest extends BaseApiTest {

    @Nested
    @DisplayName("POST /auth/login - 登录")
    class Login {

        @Test
        @DisplayName("学生登录成功")
        void studentLoginSuccess() {
            ApiResponse resp = publicApi.post("/auth/login",
                    Map.of("username", "2022001", "password", "test123"));
            resp.assertOk();
            assertNotNull(resp.getDataText("token"));
            assertEquals("ROLE_STUDENT", resp.getDataNested("userInfo", "roleCode"));
        }

        @Test
        @DisplayName("教师登录成功")
        void teacherLoginSuccess() {
            ApiResponse resp = publicApi.post("/auth/login",
                    Map.of("username", "t001", "password", "test123"));
            assertEquals(200, resp.getCode(), "教师登录失败: " + resp.getMessage());
            assertEquals("ROLE_TEACHER", resp.getDataNested("userInfo", "roleCode"));
        }

        @Test
        @DisplayName("管理员登录成功")
        void adminLoginSuccess() {
            ApiResponse resp = publicApi.post("/auth/login",
                    Map.of("username", "admin", "password", "admin123"));
            resp.assertOk();
            assertEquals("ROLE_ADMIN", resp.getDataNested("userInfo", "roleCode"));
        }

        @Test
        @DisplayName("密码错误返回 401")
        void wrongPassword() {
            ApiResponse resp = publicApi.post("/auth/login",
                    Map.of("username", "2022001", "password", "wrongpass"));
            resp.assertCode(401);
        }

        @Test
        @DisplayName("账号不存在返回 401")
        void userNotFound() {
            ApiResponse resp = publicApi.post("/auth/login",
                    Map.of("username", "nonexist", "password", "test123"));
            resp.assertCode(401);
        }
    }

    @Nested
    @DisplayName("GET /auth/user-info - 获取用户信息")
    class UserInfo {

        @Test
        @DisplayName("已登录用户获取信息")
        void getCurrentUser() {
            ApiResponse resp = studentApi.get("/auth/user-info");
            resp.assertOk();
            assertEquals("2022001", resp.getDataText("username"));
            assertEquals("张三", resp.getDataText("realName"));
        }

        @Test
        @DisplayName("未登录返回 401")
        void unauthorized() {
            ApiResponse resp = publicApi.get("/auth/user-info");
            // 未认证时应返回 401
            assertTrue(resp.getCode() == 401 || resp.getCode() == 302,
                    "期望 401 或 302，实际: " + resp.getCode());
        }
    }

    @Nested
    @DisplayName("PUT /auth/password - 修改密码")
    class ChangePassword {

        @Test
        @DisplayName("修改密码成功")
        void changePasswordSuccess() {
            ApiResponse resp = studentApi.put("/auth/password",
                    Map.of("oldPassword", "test123", "newPassword", "newpass123"));
            resp.assertOk();
            // 恢复原密码
            studentApi.put("/auth/password",
                    Map.of("oldPassword", "newpass123", "newPassword", "test123"));
        }

        @Test
        @DisplayName("原密码错误返回 400")
        void wrongOldPassword() {
            // 使用 admin token 调用 student 密码接口测试密码错误
            ApiResponse resp = studentApi.put("/auth/password",
                    Map.of("oldPassword", "wrong", "newPassword", "newpass123"));
            resp.assertCode(400);
        }
    }

    @Nested
    @DisplayName("POST /auth/logout - 退出登录")
    class Logout {

        @Test
        @DisplayName("已登录用户可正常退出")
        void logoutSuccess() {
            ApiResponse resp = studentApi.post("/auth/logout", null);
            resp.assertOk();
        }

        @Test
        @DisplayName("退出后重复使用同一 Token 被拦截")
        void replayAfterLogoutShouldBeBlocked() {
            String token = login("2022001", "test123");
            ApiResponse logoutResp = new ApiClient("http://localhost:" + port, token).post("/auth/logout", null);
            logoutResp.assertOk();

            ApiResponse replayResp = new ApiClient("http://localhost:" + port, token).get("/auth/user-info");
            assertEquals(401, replayResp.getCode(), "退出后的同一 token 应被拦截");
        }
    }

    @Nested
    @DisplayName("PUT /auth/profile - 更新个人信息")
    class Profile {

        @Test
        @DisplayName("更新手机号和邮箱")
        void updateProfile() {
            ApiResponse resp = studentApi.put("/auth/profile",
                    Map.of("phone", "13800138001", "email", "student@test.com"));
            resp.assertOk();
        }
    }

    @Nested
    @DisplayName("GET /auth/check-first-login - 首次登录检查")
    class CheckFirstLogin {

        @Test
        @DisplayName("学生首次登录标记为 true")
        void studentFirstLoginTrue() {
            ApiResponse resp = studentApi.get("/auth/check-first-login");
            resp.assertOk();
            assertTrue(resp.getDataBool("firstLogin"));
        }

        @Test
        @DisplayName("管理员首次登录标记为 false")
        void adminFirstLoginFalse() {
            ApiResponse resp = adminApi.get("/auth/check-first-login");
            resp.assertOk();
            assertFalse(resp.getDataBool("firstLogin"));
        }
    }
}

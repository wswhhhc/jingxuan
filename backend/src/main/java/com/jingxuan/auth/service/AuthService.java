package com.jingxuan.auth.service;

import com.jingxuan.auth.model.LoginRequest;
import com.jingxuan.auth.model.LoginResponse;
import com.jingxuan.auth.model.UserInfoVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 获取当前登录用户信息
     */
    UserInfoVO getCurrentUserInfo();

    /**
     * 修改密码
     */
    void changePassword(String oldPassword, String newPassword);

    /**
     * 更新个人信息
     */
    void updateProfile(java.util.Map<String, Object> body);

    /**
     * 检查当前用户是否首次登录
     */
    boolean checkFirstLogin();

    /**
     * 登出
     */
    void logout();
}

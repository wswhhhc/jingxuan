package com.jingxuan.auth.service;

import com.jingxuan.auth.model.LoginRequest;
import com.jingxuan.auth.model.LoginResponse;
import com.jingxuan.auth.model.UserInfoVO;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysRole;
import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.RoleEnum;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.exception.UnauthorizedException;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.modules.log.service.LogService;
import com.jingxuan.security.JwtTokenProvider;
import com.jingxuan.security.SecurityUtils;
import com.jingxuan.security.TokenBlacklistService;
import com.jingxuan.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDictMapper sysDictMapper;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("用户名或密码错误");
        } catch (DisabledException e) {
            throw new UnauthorizedException("账号已被禁用，请联系管理员");
        }

        SysUser user = sysUserMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new UnauthorizedException("用户不存在");
        }
        if (user.getStatus() == UserStatusEnum.DISABLED) {
            throw new UnauthorizedException("账号已被禁用，请联系管理员");
        }

        String roleCode = getRoleCode(user.getRoleId());
        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getUsername(), roleCode,
                Boolean.TRUE.equals(request.getRememberMe()));

        long expiresIn = jwtTokenProvider.getRemainingValidity(token);

        String ip = "";
        String method = "";
        String path = "";
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                ip = IpUtil.getIpAddr(req);
                method = req.getMethod();
                path = req.getRequestURI();
            }
        } catch (Exception e) {
            // 拿不到请求上下文时保持空值，不影响登录
        }

        logService.recordLog(user.getId(), user.getUsername(), "登录", "用户登录",
                user.getId(), ip, method, path, true, null, 0L);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(Math.max(expiresIn, 0))
                .userInfo(buildUserInfo(user))
                .build();
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new UnauthorizedException("用户不存在");
        }
        return buildUserInfo(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String oldPassword, String newPassword) {
        Long userId = SecurityUtils.requireCurrentUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFirstLogin(false);
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(java.util.Map<String, Object> body) {
        Long userId = SecurityUtils.requireCurrentUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (body.containsKey("avatar")) {
            user.setAvatar((String) body.get("avatar"));
        }
        if (body.containsKey("phone")) {
            user.setPhone((String) body.get("phone"));
        }
        if (body.containsKey("email")) {
            user.setEmail((String) body.get("email"));
        }
        sysUserMapper.updateById(user);
    }

    @Override
    public boolean checkFirstLogin() {
        Long userId = SecurityUtils.requireCurrentUserId();
        SysUser user = sysUserMapper.selectById(userId);
        return user != null && Boolean.TRUE.equals(user.getFirstLogin());
    }

    @Override
    public void logout() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        String bearerToken = request.getHeader(com.jingxuan.constant.SecurityConstants.TOKEN_HEADER);
        if (bearerToken == null || !bearerToken.startsWith(com.jingxuan.constant.SecurityConstants.TOKEN_PREFIX)) {
            return;
        }
        String token = bearerToken.substring(com.jingxuan.constant.SecurityConstants.TOKEN_PREFIX.length());
        long ttl = jwtTokenProvider.getRemainingValidity(token);
        tokenBlacklistService.blacklist(token, ttl);
    }

    private UserInfoVO buildUserInfo(SysUser user) {
        String roleCode = getRoleCode(user.getRoleId());
        String roleName = getRoleName(user.getRoleId());

        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roleId(user.getRoleId())
                .roleCode(roleCode)
                .roleName(roleName)
                .avatar(user.getAvatar())
                .firstLogin(Boolean.TRUE.equals(user.getFirstLogin()))
                .classId(user.getClassId())
                .className(getClassName(user.getClassId()))
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();
    }

    private String getRoleCode(Integer roleId) {
        if (roleId == null) return RoleEnum.STUDENT.getAuthority();
        SysRole role = sysRoleMapper.selectById(roleId.longValue());
        return role != null ? role.getRoleCode() : RoleEnum.STUDENT.getAuthority();
    }

    private String getRoleName(Integer roleId) {
        if (roleId == null) return RoleEnum.STUDENT.getLabel();
        SysRole role = sysRoleMapper.selectById(roleId.longValue());
        return role != null ? role.getRoleName() : RoleEnum.STUDENT.getLabel();
    }

    private String getClassName(Long classId) {
        if (classId == null) return null;
        SysDict dict = sysDictMapper.selectById(classId);
        return dict != null ? dict.getDictLabel() : null;
    }
}

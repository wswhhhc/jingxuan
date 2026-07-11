package com.jingxuan.modules.auth.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.common.Result;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.modules.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class RegistrationController {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ScoreBatchMapper scoreBatchMapper;

    @Autowired
    private SysDictMapper sysDictMapper;

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String realName = (String) body.get("realName");
        String email = (String) body.get("email");
        String verifyCode = (String) body.get("verifyCode");

        Integer roleId;
        Object roleIdObj = body.get("roleId");
        if (roleIdObj instanceof Integer) {
            roleId = (Integer) roleIdObj;
        } else if (roleIdObj instanceof Number) {
            roleId = ((Number) roleIdObj).intValue();
        } else {
            roleId = Integer.valueOf(roleIdObj.toString());
        }

        // 字段验证
        if (username == null || username.trim().isEmpty()) {
            return Result.error("请输入用户名（学号/工号）");
        }
        if (password == null || password.length() < 6) {
            return Result.error("密码至少6位");
        }
        if (realName == null || realName.trim().isEmpty()) {
            return Result.error("请输入真实姓名");
        }
        if (email == null || email.trim().isEmpty()) {
            return Result.error("请输入邮箱地址");
        }
        if (verifyCode == null || verifyCode.trim().isEmpty()) {
            return Result.error("请输入验证码");
        }
        if (roleId == null || (roleId != 1 && roleId != 2)) {
            return Result.error("请选择有效的角色（学生或教师）");
        }

        email = email.trim();
        username = username.trim().toLowerCase();
        realName = realName.trim();

        // 验证码校验
        String verifyKey = "jingxuan:verify:" + email + ":" + roleId;
        String code = redisTemplate.opsForValue().get(verifyKey);
        if (code == null) {
            return Result.error("验证码已过期，请重新发送");
        }
        if (!code.equals(verifyCode.trim())) {
            return Result.error("验证码不正确");
        }

        // 检查用户名是否已存在
        SysUser existing = sysUserMapper.findByUsername(username);
        if (existing != null) {
            return Result.error("该用户名已被注册");
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setEmail(email);
        user.setRoleId(roleId);
        user.setStatus(UserStatusEnum.ENABLED);
        user.setFirstLogin(false);

        // 校验并解析 classId（学生必选班级）
        Long classId = null;
        if (roleId == 1) {
            Object classIdObj = body.get("classId");
            if (classIdObj instanceof Number) {
                classId = ((Number) classIdObj).longValue();
            } else if (classIdObj instanceof String && !((String) classIdObj).isEmpty()) {
                classId = Long.valueOf((String) classIdObj);
            }
            if (classId == null) {
                return Result.error("学生注册请选择班级");
            }
            user.setClassId(classId);
        }

        try {
            sysUserMapper.insert(user);
            redisTemplate.delete(verifyKey);

            // 新学生注册后自动获取批次通知
            if (roleId == 1 && classId != null) {
                generateBatchNoticesForNewStudent(user.getId(), classId);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", user.getId());
            result.put("username", user.getUsername());
            result.put("realName", user.getRealName());
            return Result.ok("注册成功", result);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Duplicate")) {
                return Result.error("该邮箱或用户名已被注册");
            }
            throw e;
        }
    }

    /**
     * 为学生生成批次通知（新注册时调用）
     */
    private void generateBatchNoticesForNewStudent(Long studentId, Long classId) {
        // 查询该班级所属的 dict_value（用于匹配 classScopes）
        SysDict classDict = sysDictMapper.selectById(classId);
        if (classDict == null) return;

        String classValue = classDict.getDictValue();
        if (classValue == null || classValue.isBlank()) return;

        LocalDateTime now = LocalDateTime.now();

        // 查询所有活跃且有通知内容的批次
        List<ScoreBatch> activeBatches = scoreBatchMapper.selectList(
                Wrappers.<ScoreBatch>lambdaQuery()
                        .eq(ScoreBatch::getStatus, 1)
                        .eq(ScoreBatch::getDeleted, 0)
                        .le(ScoreBatch::getStartTime, now)
                        .ge(ScoreBatch::getEndTime, now)
                        .isNotNull(ScoreBatch::getNoticeTitle)
                        .isNotNull(ScoreBatch::getNoticeContent));

        for (ScoreBatch batch : activeBatches) {
            String scopes = batch.getClassScopes();
            if (scopes == null || scopes.isBlank()) continue;

            // 检查班级是否在批次范围内
            boolean inScope = false;
            String trimmed = scopes.trim();
            if ("全校可参与".equals(trimmed) || "全校".equals(trimmed) || "all".equalsIgnoreCase(trimmed)) {
                inScope = true;
            } else {
                Set<String> scopeValues = Arrays.stream(
                        trimmed.replace("[", "").replace("]", "").replace("\"", "").split("[,，]"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());
                inScope = scopeValues.contains(classValue);
            }

            if (inScope) {
                notificationService.sendNotification(
                        studentId,
                        batch.getNoticeTitle(),
                        batch.getNoticeContent(),
                        "BATCH_NOTICE",
                        batch.getId()
                );
            }
        }
    }
}

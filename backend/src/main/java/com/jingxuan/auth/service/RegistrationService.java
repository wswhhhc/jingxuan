package com.jingxuan.auth.service;

import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 自助注册与邮箱验证码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;

    @Value("${MAIL_FROM:}")
    private String mailFrom;

    public void sendVerificationCode(Map<String, Object> body) {
        String email = normalizeEmail(body.get("email"));
        Integer roleId = parseRoleId(body.get("roleId"));

        if (mailSender == null) {
            log.warn("邮箱服务未配置（MAIL_* 环境变量），验证码仅记录日志");
        }
        if (sysUserMapper.countByEmailAndRole(email, roleId) > 0) {
            throw new BusinessException("该邮箱已被注册");
        }

        String cooldownKey = cooldownKey(email, roleId);
        Boolean firstSend = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", SEND_COOLDOWN);
        if (Boolean.FALSE.equals(firstSend)) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(verifyKey(email, roleId), code, CODE_TTL);

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(StringUtils.hasText(mailFrom) ? mailFrom : "noreply@jingxuan.com");
                message.setTo(email);
                message.setSubject("菁选注册验证码");
                message.setText("您的注册验证码是：" + code + "，5分钟内有效。若非本人操作，请忽略本邮件。");
                mailSender.send(message);
            } catch (Exception e) {
                redisTemplate.delete(verifyKey(email, roleId));
                redisTemplate.delete(cooldownKey);
                throw new BusinessException("验证码发送失败，请稍后重试");
            }
        } else {
            log.info("[邮箱未配置] 验证码仅记录于日志: email={}, code={}", email, code);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> register(Map<String, Object> body) {
        String username = normalizeUsername(body.get("username"));
        String password = requireString(body.get("password"), "请输入密码");
        String realName = requireString(body.get("realName"), "请输入真实姓名");
        String email = normalizeEmail(body.get("email"));
        String verifyCode = requireString(body.get("verifyCode"), "请输入验证码");
        Integer roleId = parseRoleId(body.get("roleId"));

        if (password.length() < 6) {
            throw new BusinessException("密码至少6位");
        }
        String storedCode = redisTemplate.opsForValue().get(verifyKey(email, roleId));
        if (storedCode == null) {
            throw new BusinessException("验证码已过期，请重新发送");
        }
        if (!storedCode.equals(verifyCode.trim())) {
            throw new BusinessException("验证码不正确");
        }
        if (sysUserMapper.countByUsername(username) > 0) {
            throw new BusinessException("该用户名已被注册");
        }
        if (sysUserMapper.countByEmailAndRole(email, roleId) > 0) {
            throw new BusinessException("该邮箱已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setEmail(email);
        user.setRoleId(roleId);
        user.setStatus(UserStatusEnum.ENABLED);
        user.setFirstLogin(false);
        // 学生注册时必须选择班级
        Long classId = parseOptionalLong(body.get("classId"));
        if (roleId == 1 && classId == null) {
            throw new BusinessException("学生注册请选择班级");
        }
        user.setClassId(classId);
        sysUserMapper.insert(user);

        redisTemplate.delete(verifyKey(email, roleId));
        redisTemplate.delete(cooldownKey(email, roleId));

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        return result;
    }

    private String verifyKey(String email, Integer roleId) {
        return "jingxuan:verify:" + email + ":" + roleId;
    }

    private String cooldownKey(String email, Integer roleId) {
        return "jingxuan:verify:cooldown:" + email + ":" + roleId;
    }

    private String normalizeEmail(Object value) {
        String email = requireString(value, "请输入邮箱地址").toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
        return email;
    }

    private String normalizeUsername(Object value) {
        return requireString(value, "请输入用户名（学号/工号）").toLowerCase();
    }

    private String requireString(Object value, String message) {
        if (value == null || !StringUtils.hasText(value.toString())) {
            throw new BusinessException(message);
        }
        return value.toString().trim();
    }

    private Integer parseRoleId(Object value) {
        if (value == null) {
            throw new BusinessException("请选择有效的角色（学生或教师）");
        }
        Integer roleId;
        if (value instanceof Number number) {
            roleId = number.intValue();
        } else {
            try {
                roleId = Integer.valueOf(value.toString());
            } catch (NumberFormatException e) {
                throw new BusinessException("请选择有效的角色（学生或教师）");
            }
        }
        if (roleId != 1 && roleId != 2) {
            throw new BusinessException("请选择有效的角色（学生或教师）");
        }
        return roleId;
    }

    private Long parseOptionalLong(Object value) {
        if (value == null || !StringUtils.hasText(value.toString())) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException("班级参数格式不正确");
        }
    }
}

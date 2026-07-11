package com.jingxuan.modules.log.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.common.PageResult;
import com.jingxuan.common.PageUtil;
import com.jingxuan.entity.SysLog;
import com.jingxuan.entity.SysUser;
import com.jingxuan.mapper.SysLogMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.modules.log.service.LogService;
import com.jingxuan.security.SecurityUtils;
import com.jingxuan.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志 服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements LogService {

    @Override
    public PageResult<SysLog> queryLogList(int pageNum, int pageSize, String action, Long userId) {
        return PageUtil.query(pageNum, pageSize, baseMapper,
                w -> w.eq(action != null && !action.isEmpty(), SysLog::getAction, action)
                      .eq(userId != null, SysLog::getUserId, userId)
                      .orderByDesc(SysLog::getCreateTime));
    }

    private final SysUserMapper sysUserMapper;

    @Override
    public void recordAction(String action, String target, Long targetId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return;
        SysUser user = sysUserMapper.selectById(userId);

        String ip = "";
        String method = "";
        String path = "";
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ip = IpUtil.getIpAddr(request);
                method = request.getMethod();
                path = request.getRequestURI();
            }
        } catch (Exception e) {
            log.trace("无法获取HTTP请求上下文: {}", e.getMessage());
        }

        recordLog(userId, user != null ? user.getUsername() : "",
                action, target, targetId, ip, method, path, true, null, 0L);
    }

    @Override
    public void recordLog(Long userId, String username, String action, String target, Long targetId,
                          String ip, String requestMethod, String requestPath,
                          boolean result, String errorMsg, Long duration) {
        SysLog logRecord = new SysLog();
        logRecord.setUserId(userId);
        logRecord.setUsername(username);
        logRecord.setAction(action);
        logRecord.setTarget(target);
        logRecord.setTargetId(targetId);
        logRecord.setIp(ip);
        logRecord.setRequestMethod(requestMethod);
        logRecord.setRequestPath(requestPath);
        logRecord.setParams("");
        logRecord.setResult(result ? 1 : 0);
        logRecord.setErrorMsg(errorMsg);
        logRecord.setDuration(duration);
        try {
            baseMapper.insert(logRecord);
        } catch (Exception e) {
            log.warn("写入操作日志失败，不影响主业务: {}", e.getMessage());
        }
    }
}

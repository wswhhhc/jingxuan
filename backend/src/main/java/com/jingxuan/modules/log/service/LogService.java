package com.jingxuan.modules.log.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SysLog;

public interface LogService extends IService<SysLog> {

    /**
     * 分页查询操作日志
     */
    PageResult<SysLog> queryLogList(int pageNum, int pageSize, String action, Long userId);

    /**
     * 记录操作日志
     */
    void recordLog(Long userId, String username, String action, String target, Long targetId,
                   String ip, String requestMethod, String requestPath, boolean result, String errorMsg, Long duration);

    /**
     * 记录操作日志（便捷方法，自动获取当前用户信息）
     */
    void recordAction(String action, String target, Long targetId);
}

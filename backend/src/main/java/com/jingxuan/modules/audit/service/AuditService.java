package com.jingxuan.modules.audit.service;

import com.jingxuan.common.PageResult;
import com.jingxuan.modules.audit.dto.AuditRequest;
import com.jingxuan.modules.audit.dto.AuditHistoryVO;

public interface AuditService {

    /**
     * 审核通过
     */
    void approve(AuditRequest request);

    /**
     * 审核驳回
     */
    void reject(AuditRequest request);

    /**
     * 查询审核记录列表
     */
    PageResult<AuditHistoryVO> queryHistory(Long workId, int pageNum, int pageSize);
}

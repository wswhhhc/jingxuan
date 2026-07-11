package com.jingxuan.modules.deleterequest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.DeleteRequest;

public interface DeleteRequestService extends IService<DeleteRequest> {

    /**
     * 学生提交删除申请
     */
    Long submitRequest(Long workId, Long studentId, String reason);

    /**
     * 管理员获取删除申请列表
     */
    PageResult<DeleteRequest> queryRequests(int pageNum, int pageSize, Integer status);

    /**
     * 管理员同意删除申请
     */
    void approve(Long requestId, Long adminId);

    /**
     * 管理员拒绝删除申请
     */
    void reject(Long requestId, Long adminId, String reply);
}

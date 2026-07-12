package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.entity.SysUser;
import com.jingxuan.enums.UserStatusEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.exception.NotFoundException;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.modules.log.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 教师自助注册审核用例。 */
@Service
@RequiredArgsConstructor
public class UserApprovalService {

    private final SysUserMapper sysUserMapper;
    private final LogService logService;

    @Transactional(rollbackFor = Exception.class)
    public void decide(Long userId, String decision, String reason) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        if (user.getRoleId() == null || user.getRoleId() != 2
                || user.getStatus() != UserStatusEnum.PENDING_APPROVAL) {
            throw new BusinessException(409, "只能审批待审核的教师账号");
        }

        if ("APPROVED".equals(decision)) {
            user.setStatus(UserStatusEnum.ENABLED);
            sysUserMapper.updateById(user);
            logService.recordAction("USER_APPROVAL_APPROVED", "TEACHER", userId);
            return;
        }
        if ("REJECTED".equals(decision)) {
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException(422, "驳回教师申请必须填写原因");
            }
            user.setStatus(UserStatusEnum.DISABLED);
            sysUserMapper.updateById(user);
            logService.recordAction("USER_APPROVAL_REJECTED", "TEACHER;reason=" + safeReason(reason), userId);
            return;
        }
        throw new BusinessException(422, "decision 必须为 APPROVED 或 REJECTED");
    }

    private static String safeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "";
        }
        return reason.trim().replaceAll("[\\r\\n\\t]+", " ");
    }
}

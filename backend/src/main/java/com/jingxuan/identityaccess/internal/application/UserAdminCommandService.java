package com.jingxuan.identityaccess.internal.application;

import com.jingxuan.entity.SysUser;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.identityaccess.api.V1AiUserImportRequest;
import com.jingxuan.identityaccess.api.V1AiUserImportResponse;
import com.jingxuan.identityaccess.api.V1BatchImportResult;
import com.jingxuan.identityaccess.api.V1UserRequest;
import com.jingxuan.modules.userimport.dto.AiImportMessage;
import com.jingxuan.modules.userimport.dto.AiUserImportRequest;
import com.jingxuan.modules.userimport.service.AiUserImportService;
import com.jingxuan.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 用户管理命令用例 — 委托给现有 SysUserService。 */
@Service
@RequiredArgsConstructor
public class UserAdminCommandService {

    private static final int MAX_BATCH_CREATE_SIZE = 100;

    private final SysUserService sysUserService;
    private final AiUserImportService aiUserImportService;

    @Transactional
    public void createUser(V1UserRequest request) {
        SysUser user = toEntity(request);
        sysUserService.createUser(user);
    }

    @Transactional
    public void updateUser(Long id, V1UserRequest request) {
        SysUser user = toEntity(request);
        user.setId(id);
        sysUserService.updateUser(user);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        sysUserService.updateStatus(id, status);
    }

    @Transactional
    public void deleteUser(Long id) {
        sysUserService.deleteUser(id);
    }

    @Transactional
    public V1BatchImportResult batchCreate(List<V1UserRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException("导入用户列表不能为空");
        }
        if (requests.size() > MAX_BATCH_CREATE_SIZE) {
            throw new BusinessException("单次最多导入100个用户");
        }
        int success = 0;
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            V1UserRequest req = requests.get(index);
            if (req == null) {
                errors.add("第" + (index + 1) + "条: 用户不能为空");
                continue;
            }
            try {
                SysUser user = toEntity(req);
                sysUserService.createUser(user);
                success++;
            } catch (Exception e) {
                String itemName = req.username() == null || req.username().isBlank()
                        ? "第" + (index + 1) + "条"
                        : req.username();
                errors.add(itemName + ": " + e.getMessage());
            }
        }
        return new V1BatchImportResult(success, errors.size(), errors);
    }

    public V1AiUserImportResponse aiParse(V1AiUserImportRequest request) {
        AiUserImportRequest old = new AiUserImportRequest();
        if (request.messages() != null) {
            old.setMessages(request.messages().stream()
                    .map(m -> {
                        AiImportMessage msg = new AiImportMessage();
                        msg.setRole(m.role());
                        msg.setContent(m.content());
                        return msg;
                    })
                    .toList());
        }
        return V1AiUserImportResponse.from(aiUserImportService.parse(old));
    }

    private static SysUser toEntity(V1UserRequest request) {
        SysUser user = new SysUser();
        user.setUsername(request.username().trim());
        user.setPassword(request.password());
        user.setRealName(request.realName() != null ? request.realName().trim() : null);
        user.setRoleId(request.roleId());
        user.setClassId(request.classId());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        return user;
    }
}

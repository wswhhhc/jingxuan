package com.jingxuan.identityaccess.internal.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.entity.DeleteRequest;
import com.jingxuan.entity.StudentTask;
import com.jingxuan.entity.SysNotification;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkMember;
import com.jingxuan.enums.RoleEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.exception.NotFoundException;
import com.jingxuan.identityaccess.api.V1UserDeletionImpact;
import com.jingxuan.mapper.DeleteRequestMapper;
import com.jingxuan.mapper.StudentTaskMapper;
import com.jingxuan.mapper.SysNotificationMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkMemberMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 用户物理删除的影响预览和受控执行用例。 */
@Service
public class UserDeletionService {

    private final SysUserMapper users;
    private final DeleteRequestMapper deleteRequests;
    private final StudentTaskMapper tasks;
    private final SysNotificationMapper notifications;
    private final WorkMapper works;
    private final WorkMemberMapper members;

    public UserDeletionService(SysUserMapper users, DeleteRequestMapper deleteRequests, StudentTaskMapper tasks,
                               SysNotificationMapper notifications, WorkMapper works, WorkMemberMapper members) {
        this.users = users;
        this.deleteRequests = deleteRequests;
        this.tasks = tasks;
        this.notifications = notifications;
        this.works = works;
        this.members = members;
    }

    public V1UserDeletionImpact impact(Long userId) {
        requiredUser(userId);
        long deletionRequests = count(deleteRequests.selectCount(
                Wrappers.<DeleteRequest>lambdaQuery().eq(DeleteRequest::getStudentId, userId)));
        long studentTasks = count(tasks.selectCount(
                Wrappers.<StudentTask>lambdaQuery().eq(StudentTask::getUserId, userId)));
        long userNotifications = count(notifications.selectCount(
                Wrappers.<SysNotification>lambdaQuery().eq(SysNotification::getUserId, userId)));
        long submittedWorks = count(works.selectCount(
                Wrappers.<Work>lambdaQuery().eq(Work::getSubmitterId, userId)));
        long workMemberships = count(members.selectCount(
                Wrappers.<WorkMember>lambdaQuery().eq(WorkMember::getStudentId, userId)));

        List<String> references = new ArrayList<>();
        add(references, "delete_request", deletionRequests);
        add(references, "student_task", studentTasks);
        add(references, "sys_notification", userNotifications);
        add(references, "work.submitter", submittedWorks);
        add(references, "work_member", workMemberships);
        return new V1UserDeletionImpact("user", userId.toString(), deletionRequests + studentTasks + userNotifications
                + submittedWorks + workMemberships, List.copyOf(references), submittedWorks > 0 || workMemberships > 0);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, boolean confirmed) {
        SysUser user = requiredUser(userId);
        ensureNotRootAdministrator(user);
        V1UserDeletionImpact impact = impact(userId);
        if (impact.deletionBlocked()) {
            throw new BusinessException(409, "用户仍关联作品，请先处理作品归属或删除作品");
        }
        if (impact.referenceCount() > 0 && !confirmed) {
            throw new BusinessException(409, "用户仍有关联数据，请先确认删除影响");
        }

        deleteRequests.physicalDeleteByStudentId(userId);
        tasks.physicalDeleteByUserId(userId);
        notifications.physicalDeleteByUserId(userId);
        if (users.physicalDeleteById(userId) != 1) {
            throw new NotFoundException("用户不存在");
        }
    }

    private SysUser requiredUser(Long userId) {
        SysUser user = users.selectById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return user;
    }

    private static void ensureNotRootAdministrator(SysUser user) {
        if (user.getRoleId() != null && user.getRoleId() == RoleEnum.ADMIN.getValue()
                && "admin".equalsIgnoreCase(user.getUsername())) {
            throw new BusinessException(403, "系统管理员不允许删除");
        }
    }

    private static long count(Long value) {
        return value == null ? 0 : value;
    }

    private static void add(List<String> references, String name, long value) {
        if (value > 0) {
            references.add(name + ": " + value);
        }
    }
}

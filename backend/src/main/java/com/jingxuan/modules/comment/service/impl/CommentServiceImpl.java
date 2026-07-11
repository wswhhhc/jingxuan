package com.jingxuan.modules.comment.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.WorkMember;
import com.jingxuan.entity.SysRole;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.WorkComment;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkPublish;
import com.jingxuan.enums.RoleEnum;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkCommentMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkMemberMapper;
import com.jingxuan.mapper.WorkPublishMapper;
import com.jingxuan.modules.comment.dto.AdminCommentVO;
import com.jingxuan.modules.comment.dto.CommentVO;
import com.jingxuan.modules.comment.service.CommentService;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.sensitive.service.DeepSeekReviewService;
import com.jingxuan.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl extends ServiceImpl<WorkCommentMapper, WorkComment> implements CommentService {

    private final DeepSeekReviewService deepSeekReviewService;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final WorkMapper workMapper;
    private final WorkMemberMapper workMemberMapper;
    private final WorkPublishMapper workPublishMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(Long workId, Long userId, String content, Long parentId, String guestName) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException("评论内容不能超过500字");
        }
        DeepSeekReviewService.ReviewResult review = deepSeekReviewService.review(content, "comment");
        if (!review.isPassed()) {
            throw new BusinessException("评论内容违规：" + review.getReason());
        }

        // 校验作品存在且已发布
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        WorkPublish publish = workPublishMapper.selectByWorkId(workId);
        if (publish == null || publish.getPublishStatus() != 1) {
            throw new BusinessException("该作品暂未发布，无法评论");
        }

        // 校验 parentId 合法性：存在且属于同一作品
        if (parentId != null) {
            WorkComment parent = baseMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException("回复的评论不存在");
            }
            if (!workId.equals(parent.getWorkId())) {
                throw new BusinessException("不能跨作品回复评论");
            }
        }

        WorkComment comment = new WorkComment();
        comment.setWorkId(workId);
        comment.setUserId(userId);
        comment.setGuestName(userId == null ? guestName : null);
        comment.setContent(content);
        comment.setParentId(parentId);
        baseMapper.insert(comment);

        if (parentId != null) {
            WorkComment parent = baseMapper.selectById(parentId);
            if (parent != null && parent.getUserId() != null && !Objects.equals(parent.getUserId(), userId)) {
                String replierName;
                if (userId != null) {
                    SysUser replier = sysUserMapper.selectById(userId);
                    replierName = replier != null && replier.getRealName() != null && !replier.getRealName().isBlank()
                            ? replier.getRealName()
                            : "有人";
                } else {
                    replierName = (guestName != null && !guestName.isBlank()) ? guestName : "游客";
                }
                notificationService.sendNotification(
                        parent.getUserId(),
                        "评论收到回复",
                        replierName + " 回复了你在《" + work.getTitle() + "》下的评论",
                        "comment",
                        workId
                );
            }
        }

        return comment.getId();
    }

    @Override
    public PageResult<CommentVO> getWorkCommentsWithUserInfo(Long workId, int pageNum, int pageSize) {
        // 1. 分页查询顶级评论（parentId IS NULL），按创建时间倒序
        Page<WorkComment> topPage = new Page<>(pageNum, pageSize);
        Page<WorkComment> topResult = baseMapper.selectPage(topPage,
                Wrappers.<WorkComment>lambdaQuery()
                        .eq(WorkComment::getWorkId, workId)
                        .isNull(WorkComment::getParentId)
                        .orderByDesc(WorkComment::getCreateTime));

        List<WorkComment> topComments = topResult.getRecords();
        if (topComments.isEmpty()) {
            return PageResult.of(Collections.emptyList(), topResult.getTotal(), pageNum, pageSize);
        }

        Map<Long, WorkComment> commentMap = new HashMap<>();
        topComments.forEach(comment -> commentMap.put(comment.getId(), comment));

        // 2. 查询该作品下所有非顶级评论（parentId IS NOT NULL），按创建时间正序
        List<WorkComment> allReplies = baseMapper.selectList(
                Wrappers.<WorkComment>lambdaQuery()
                        .eq(WorkComment::getWorkId, workId)
                        .isNotNull(WorkComment::getParentId)
                        .orderByAsc(WorkComment::getCreateTime));
        allReplies.forEach(comment -> commentMap.put(comment.getId(), comment));

        // 3. 批量查询用户信息
        Set<Long> allUserIds = new HashSet<>();
        topComments.forEach(c -> { if (c.getUserId() != null) allUserIds.add(c.getUserId()); });
        allReplies.forEach(c -> { if (c.getUserId() != null) allUserIds.add(c.getUserId()); });

        Map<Long, SysUser> userMap = allUserIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(allUserIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
        Map<Long, String> roleNameMap = sysRoleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName, (a, b) -> a));

        // 4. 将所有回复按 parentId 分组并构建 VO
        Map<Long, List<CommentVO>> repliesByParentId = allReplies.stream()
                .map(r -> buildCommentVO(r, userMap, roleNameMap, commentMap))
                .collect(Collectors.groupingBy(CommentVO::getParentId));

        // 5. 递归设置子回复（处理多级嵌套）
        for (List<CommentVO> replies : repliesByParentId.values()) {
            for (CommentVO reply : replies) {
                reply.setReplies(repliesByParentId.getOrDefault(reply.getId(), Collections.emptyList()));
            }
        }

        // 6. 组装顶级评论（含完整的回复树）
        List<CommentVO> voList = topComments.stream().map(c -> {
            CommentVO vo = buildCommentVO(c, userMap, roleNameMap, commentMap);
            vo.setReplies(repliesByParentId.getOrDefault(c.getId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(voList, topResult.getTotal(), pageNum, pageSize);
    }

    @Override
    public PageResult<AdminCommentVO> getAdminComments(int pageNum, int pageSize, Long workId, String userKeyword, String contentKeyword) {
        Page<WorkComment> page = new Page<>(pageNum, pageSize);
        var query = Wrappers.<WorkComment>lambdaQuery()
                .orderByDesc(WorkComment::getCreateTime);

        if (workId != null) {
            query.eq(WorkComment::getWorkId, workId);
        }
        if (contentKeyword != null && !contentKeyword.trim().isEmpty()) {
            query.like(WorkComment::getContent, contentKeyword.trim());
        }
        if (userKeyword != null && !userKeyword.trim().isEmpty()) {
            String trimmedKeyword = userKeyword.trim();
            List<Long> matchedUserIds = sysUserMapper.selectList(
                    Wrappers.<SysUser>lambdaQuery()
                            .and(wrapper -> wrapper.like(SysUser::getRealName, trimmedKeyword)
                                    .or().like(SysUser::getUsername, trimmedKeyword))
                            .select(SysUser::getId))
                    .stream()
                    .map(SysUser::getId)
                    .collect(Collectors.toList());
            if (matchedUserIds.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0, pageNum, pageSize);
            }
            query.in(WorkComment::getUserId, matchedUserIds);
        }

        Page<WorkComment> result = baseMapper.selectPage(page, query);
        List<WorkComment> comments = result.getRecords();
        if (comments.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), pageNum, pageSize);
        }

        Set<Long> workIds = comments.stream().map(WorkComment::getWorkId).collect(Collectors.toSet());
        Set<Long> userIds = comments.stream()
                .map(WorkComment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> parentIds = comments.stream()
                .map(WorkComment::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Work> workMap = workMapper.selectBatchIds(workIds).stream()
                .collect(Collectors.toMap(Work::getId, Function.identity(), (a, b) -> a));

        List<WorkComment> parentComments = parentIds.isEmpty()
                ? Collections.emptyList()
                : baseMapper.selectBatchIds(parentIds);
        parentComments.forEach(parent -> { if (parent.getUserId() != null) userIds.add(parent.getUserId()); });
        Map<Long, WorkComment> parentCommentMap = parentComments.stream()
                .collect(Collectors.toMap(WorkComment::getId, Function.identity(), (a, b) -> a));

        Map<Long, SysUser> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
        Map<Long, String> roleNameMap = sysRoleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName, (a, b) -> a));

        List<AdminCommentVO> records = comments.stream().map(comment -> {
            AdminCommentVO vo = new AdminCommentVO();
            vo.setId(comment.getId());
            vo.setWorkId(comment.getWorkId());
            vo.setContent(comment.getContent());
            vo.setParentId(comment.getParentId());
            vo.setUserId(comment.getUserId());
            vo.setGuestName(comment.getGuestName());
            vo.setCreateTime(comment.getCreateTime());

            Work work = workMap.get(comment.getWorkId());
            if (work != null) {
                vo.setWorkTitle(work.getTitle());
            }

            if (comment.getUserId() != null) {
                SysUser user = userMap.get(comment.getUserId());
                if (user != null) {
                    vo.setUserName(user.getRealName());
                    if (user.getRoleId() != null) {
                        vo.setRoleName(roleNameMap.getOrDefault(user.getRoleId().longValue(), ""));
                    }
                }
            } else {
                vo.setUserName(comment.getGuestName() != null && !comment.getGuestName().isBlank()
                        ? comment.getGuestName() + "·游客" : "游客");
                vo.setRoleName("游客");
            }

            if (comment.getParentId() != null) {
                WorkComment parent = parentCommentMap.get(comment.getParentId());
                if (parent != null) {
                    if (parent.getUserId() != null) {
                        SysUser replyToUser = userMap.get(parent.getUserId());
                        if (replyToUser != null) {
                            vo.setReplyToUserName(replyToUser.getRealName());
                        }
                    } else {
                        vo.setReplyToUserName(parent.getGuestName() != null && !parent.getGuestName().isBlank()
                                ? parent.getGuestName() + "·游客" : "游客");
                    }
                }
            }
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long operatorId, String operatorRoleCode) {
        WorkComment comment = baseMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在或已删除");
        }
        if (!hasDeletePermission(comment, operatorId, operatorRoleCode)) {
            throw new BusinessException("无权删除该评论");
        }

        List<WorkComment> workComments = baseMapper.selectList(
                Wrappers.<WorkComment>lambdaQuery()
                        .eq(WorkComment::getWorkId, comment.getWorkId())
                        .select(WorkComment::getId, WorkComment::getParentId));

        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (WorkComment workComment : workComments) {
            if (workComment.getParentId() != null) {
                childrenMap.computeIfAbsent(workComment.getParentId(), key -> new ArrayList<>())
                        .add(workComment.getId());
            }
        }

        LinkedHashSet<Long> deleteIds = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(commentId);
        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            if (!deleteIds.add(currentId)) {
                continue;
            }
            for (Long childId : childrenMap.getOrDefault(currentId, Collections.emptyList())) {
                queue.add(childId);
            }
        }

        if (!deleteIds.isEmpty()) {
            removeByIds(deleteIds);
        }
    }

    private CommentVO buildCommentVO(WorkComment c,
                                     Map<Long, SysUser> userMap,
                                     Map<Long, String> roleNameMap,
                                     Map<Long, WorkComment> commentMap) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setWorkId(c.getWorkId());
        vo.setUserId(c.getUserId());
        vo.setGuestName(c.getGuestName());
        vo.setContent(c.getContent());
        vo.setParentId(c.getParentId());
        vo.setCreateTime(c.getCreateTime());
        if (c.getUserId() != null) {
            SysUser user = userMap.get(c.getUserId());
            if (user != null) {
                vo.setUserName(user.getRealName());
                vo.setAvatarUrl(user.getAvatar());
                vo.setRoleName(user.getRoleId() != null ? roleNameMap.getOrDefault(user.getRoleId().longValue(), "") : "");
            }
        } else {
            vo.setUserName(c.getGuestName() != null && !c.getGuestName().isBlank() ? c.getGuestName() + "·游客" : "游客");
            vo.setRoleName("游客");
        }
        if (c.getParentId() != null) {
            WorkComment parent = commentMap.get(c.getParentId());
            if (parent != null) {
                if (parent.getUserId() != null) {
                    SysUser replyToUser = userMap.get(parent.getUserId());
                    if (replyToUser != null) {
                        vo.setReplyToUserName(replyToUser.getRealName());
                    }
                } else {
                    vo.setReplyToUserName(parent.getGuestName() != null && !parent.getGuestName().isBlank()
                            ? parent.getGuestName() + "·游客" : "游客");
                }
            }
        }
        return vo;
    }

    private boolean hasDeletePermission(WorkComment comment, Long operatorId, String operatorRoleCode) {
        if (operatorId == null) {
            return false;
        }
        if (operatorId.equals(comment.getUserId())) {
            return true;
        }
        if (RoleEnum.ADMIN.getAuthority().equals(operatorRoleCode)) {
            return true;
        }
        WorkMember leader = workMemberMapper.selectOne(
                Wrappers.<WorkMember>lambdaQuery()
                        .eq(WorkMember::getWorkId, comment.getWorkId())
                        .eq(WorkMember::getStudentId, operatorId)
                        .eq(WorkMember::getIsLeader, 1)
                        .last("limit 1"));
        return leader != null;
    }

    @Override
    public PageResult<WorkComment> getWorkComments(Long workId, int pageNum, int pageSize) {
        Page<WorkComment> page = new Page<>(pageNum, pageSize);
        Page<WorkComment> result = baseMapper.selectPage(page,
                Wrappers.<WorkComment>lambdaQuery()
                        .eq(WorkComment::getWorkId, workId)
                        .orderByDesc(WorkComment::getCreateTime));
        return PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }
}

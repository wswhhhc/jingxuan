package com.jingxuan.modules.work.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.RewardIssue;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkAttachment;
import com.jingxuan.entity.WorkMember;
import com.jingxuan.entity.WorkPublish;
import com.jingxuan.entity.WorkTag;
import com.jingxuan.enums.AuditStatusEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.RewardIssueMapper;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkAttachmentMapper;
import com.jingxuan.mapper.WorkAuditMapper;
import com.jingxuan.mapper.WorkCommentMapper;
import com.jingxuan.mapper.WorkLikeMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkMemberMapper;
import com.jingxuan.mapper.WorkPublishMapper;
import com.jingxuan.mapper.WorkScoreMapper;
import com.jingxuan.mapper.WorkTagMapper;
import com.jingxuan.modules.log.service.LogService;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.score.service.ScoreService;
import com.jingxuan.modules.task.service.StudentTaskService;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkListVO;
import com.jingxuan.modules.work.dto.WorkMemberDTO;
import com.jingxuan.modules.work.dto.WorkQueryRequest;
import com.jingxuan.modules.work.dto.WorkRequest;
import com.jingxuan.modules.work.service.WorkMemberService;
import com.jingxuan.modules.work.service.WorkService;
import com.jingxuan.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 作品管理 服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class WorkServiceImpl extends ServiceImpl<WorkMapper, Work> implements WorkService {

    private final SysUserMapper sysUserMapper;
    private final WorkMemberService workMemberService;
    private final WorkMemberMapper workMemberMapper;
    private final WorkAttachmentMapper workAttachmentMapper;
    private final WorkPublishMapper workPublishMapper;
    private final ScoreBatchMapper scoreBatchMapper;
    private final LogService logService;
    private final WorkContentReviewService workContentReviewService;
    private final WorkAttachmentBindingService workAttachmentBindingService;
    private final WorkMemberPolicyService workMemberPolicyService;
    private final WorkQueryValidator workQueryValidator;
    private final StudentTaskService studentTaskService;
    private final NotificationService notificationService;
    private final WorkAuditMapper workAuditMapper;
    private final WorkCommentMapper workCommentMapper;
    private final WorkLikeMapper workLikeMapper;
    private final WorkScoreMapper workScoreMapper;
    private final WorkTagMapper workTagMapper;
    private final RewardIssueMapper rewardIssueMapper;
    @org.springframework.beans.factory.annotation.Autowired
    private ScoreService scoreService;

    @Override
    public Long createWork(WorkRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException("作品名称不能为空");
        }
        Long currentUserId = SecurityUtils.requireCurrentUserId();

        // 确定目标批次：优先使用前端传入的 batchId，未指定则取最新活跃批次
        ScoreBatch targetBatch = resolveTargetBatch(request.getBatchId());
        // 无论是否有活跃批次，都先回填已注册成员的 studentId（两者后续用途不同：studentId 用于成员识别与权限校验，批次唯一性校验仅在有活跃批次时执行）
        if (CollectionUtil.isNotEmpty(request.getMembers())) {
            workMemberPolicyService.resolveMemberStudentIds(request.getMembers());
        }

        if (targetBatch != null) {
            // 检查提交者是否已有作品在该批次中
            Long submitterCount = baseMapper.selectCount(
                    Wrappers.<Work>lambdaQuery()
                            .eq(Work::getSubmitterId, currentUserId)
                            .eq(Work::getBatchId, targetBatch.getId()));
            if (submitterCount > 0) {
                throw new BusinessException("您在该评分批次中已有作品，每个学生只能提交一个作品");
            }
            // 检查注册成员是否已在同一批次的其他作品中
            workMemberPolicyService.ensureMembersAvailableInBatch(request.getMembers(), targetBatch.getId(), null);
        }

        // 内容安全审核：作品标题、简介、运行说明
        workContentReviewService.review(request.getTitle(), request.getSummary(), request.getRunDesc());

        Work work = new Work();
        work.setTitle(request.getTitle());
        work.setSummary(request.getSummary());
        work.setTechStack(request.getTechStack());
        work.setAdvisor(request.getAdvisor());
        work.setCoverUrl(request.getCoverUrl());
        work.setPreviewUrl(normalizePreviewUrl(request.getPreviewUrl()));
        work.setRunDesc(request.getRunDesc());
        work.setStatus(AuditStatusEnum.DRAFT.getValue());
        work.setSubmitterId(currentUserId);
        if (targetBatch != null) {
            work.setBatchId(targetBatch.getId());
        }
        baseMapper.insert(work);

        // 保存团队成员
        if (CollectionUtil.isNotEmpty(request.getMembers())) {
            workMemberService.saveMembers(work.getId(), request.getMembers());
        }

        // 校验附件归属：只允许绑定未被其他作品占用的附件
        workAttachmentBindingService.bindNewAttachments(work.getId(), request.getAttachmentIds());
        work.setVideoUrl(resolveBoundMp4Url(work.getId()));
        baseMapper.updateById(work);

        return work.getId();
    }

    @Override
    public void updateWork(Long id, WorkRequest request) {
        Work work = baseMapper.selectById(id);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        int status = work.getStatus();
        if (status != AuditStatusEnum.DRAFT.getValue()
                && status != AuditStatusEnum.REJECTED.getValue()) {
            throw new BusinessException("当前状态不允许编辑");
        }
        checkOwnership(work);

        // 更新字段：null 表示前端未传，空字符串表示显式清空
        if (request.getTitle() != null) {
            work.setTitle(request.getTitle());
        }
        if (request.getSummary() != null) {
            work.setSummary(request.getSummary());
        }
        if (request.getTechStack() != null) {
            work.setTechStack(request.getTechStack());
        }
        if (request.getAdvisor() != null) {
            work.setAdvisor(request.getAdvisor());
        }
        if (request.getCoverUrl() != null) {
            work.setCoverUrl(request.getCoverUrl());
        }
        if (request.getPreviewUrl() != null) {
            work.setPreviewUrl(normalizePreviewUrl(request.getPreviewUrl()));
        }
        if (request.getRunDesc() != null) {
            work.setRunDesc(request.getRunDesc());
        }

        // 内容安全审核（编辑后的文本，在入库前检查）
        workContentReviewService.review(work.getTitle(), work.getSummary(), work.getRunDesc());

        baseMapper.updateById(work);

        // 重新保存成员
        if (request.getMembers() != null) {
            // 无论是否有活跃批次，都先回填已注册成员的 studentId（后续 saveMembers 会将其持久化到 work_member.student_id）
            workMemberPolicyService.resolveMemberStudentIds(request.getMembers());

            // 仅当当前作品已归属批次时，校验同批次成员唯一性。
            // 兼容历史草稿：batchId 为空的作品可继续编辑，提交时再做批次唯一性校验。
            Long batchIdForCheck = work.getBatchId();
            workMemberPolicyService.ensureMembersAvailableInBatch(request.getMembers(), batchIdForCheck, id);
            workMemberService.saveMembers(id, request.getMembers());
        }

        // 差量更新附件关联
        if (request.getAttachmentIds() != null) {
            workAttachmentBindingService.replaceBindings(id, request.getAttachmentIds());
            work.setVideoUrl(resolveBoundMp4Url(id));
            baseMapper.updateById(work);
        }
    }

    @Override
    public void submitWork(Long id) {
        Work work = baseMapper.selectById(id);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        if (work.getStatus() != AuditStatusEnum.DRAFT.getValue()
                && work.getStatus() != AuditStatusEnum.REJECTED.getValue()) {
            throw new BusinessException("仅草稿或已驳回状态的作品可以提交审核");
        }
        checkOwnership(work);

        work.setPreviewUrl(validatePreviewUrlForSubmission(work.getPreviewUrl()));
        ensureRequiredSubmissionFiles(id);
        work.setVideoUrl(resolveBoundMp4Url(id));

        // 自动关联活跃批次（兼容旧数据——创建时未设置 batchId 的作品）
        if (work.getBatchId() == null) {
            ScoreBatch activeBatch = scoreBatchMapper.selectOne(
                    Wrappers.<ScoreBatch>lambdaQuery()
                            .eq(ScoreBatch::getStatus, 1)
                            .orderByDesc(ScoreBatch::getCreateTime)
                            .last("LIMIT 1"));
            if (activeBatch != null) {
                Long submitterCount = baseMapper.selectCount(
                        Wrappers.<Work>lambdaQuery()
                                .eq(Work::getSubmitterId, work.getSubmitterId())
                                .eq(Work::getBatchId, activeBatch.getId())
                                .ne(Work::getId, id));
                if (submitterCount > 0) {
                    throw new BusinessException("您在当前评分批次中已有作品，每个学生只能提交一个作品");
                }
                work.setBatchId(activeBatch.getId());
            }
        }

        // 内容安全审核
        workContentReviewService.review(work.getTitle(), work.getSummary(), work.getRunDesc());

        work.setStatus(AuditStatusEnum.SUBMITTED.getValue());
        work.setSubmitTime(LocalDateTime.now());
        baseMapper.updateById(work);

        // 提交审核后，关联的待办标记为已完成
        if (work.getBatchId() != null) {
            com.jingxuan.entity.StudentTask task = studentTaskService.getByUserAndBatch(work.getSubmitterId(), work.getBatchId());
            if (task != null && task.getStatus() != 1) {
                studentTaskService.completeTask(task.getId(), id);
            }
        }

        logService.recordAction("提交审核", "作品", id);
    }

    @Override
    public void deleteWork(Long id) {
        Work work = baseMapper.selectById(id);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        int status = work.getStatus();
        if (status != AuditStatusEnum.DRAFT.getValue()
                && status != AuditStatusEnum.REJECTED.getValue()) {
            throw new BusinessException("仅草稿或已驳回状态的作品可以删除");
        }
        checkOwnership(work);

        // 解除附件关联，释放附件供后续作品复用
        workAttachmentBindingService.releaseAll(id);

        // 逻辑删除（基于 BaseEntity @TableLogic 注解）
        baseMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteWork(Long workId) {
        Work work = baseMapper.selectById(workId);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }

        // 清理所有关联数据
        workAttachmentMapper.delete(Wrappers.<WorkAttachment>lambdaQuery().eq(WorkAttachment::getWorkId, workId));
        workAuditMapper.delete(Wrappers.<com.jingxuan.entity.WorkAudit>lambdaQuery()
                .eq(com.jingxuan.entity.WorkAudit::getWorkId, workId));
        workCommentMapper.delete(Wrappers.<com.jingxuan.entity.WorkComment>lambdaQuery()
                .eq(com.jingxuan.entity.WorkComment::getWorkId, workId));
        workLikeMapper.delete(Wrappers.<com.jingxuan.entity.WorkLike>lambdaQuery()
                .eq(com.jingxuan.entity.WorkLike::getWorkId, workId));
        workMemberMapper.delete(Wrappers.<WorkMember>lambdaQuery().eq(WorkMember::getWorkId, workId));
        workPublishMapper.delete(Wrappers.<WorkPublish>lambdaQuery().eq(WorkPublish::getWorkId, workId));
        workScoreMapper.delete(Wrappers.<com.jingxuan.entity.WorkScore>lambdaQuery()
                .eq(com.jingxuan.entity.WorkScore::getWorkId, workId));
        workTagMapper.delete(Wrappers.<com.jingxuan.entity.WorkTag>lambdaQuery()
                .eq(com.jingxuan.entity.WorkTag::getWorkId, workId));
        rewardIssueMapper.delete(Wrappers.<com.jingxuan.entity.RewardIssue>lambdaQuery()
                .eq(com.jingxuan.entity.RewardIssue::getWorkId, workId));

        // 重置关联的学生待办为待处理（可重新提交）
        if (work.getBatchId() != null) {
            com.jingxuan.entity.StudentTask taskRef = studentTaskService
                    .getByUserAndBatch(work.getSubmitterId(), work.getBatchId());
            if (taskRef != null) {
                taskRef.setStatus(0);
                taskRef.setWorkId(null);
                studentTaskService.updateById(taskRef);
                log.info("待办已重置为待处理: taskId={}, batchId={}", taskRef.getId(), work.getBatchId());
            }
        }

        // 通知作者
        try {
            notificationService.sendNotification(
                    work.getSubmitterId(),
                    "作品已被删除",
                    "您的作品《" + work.getTitle() + "》已被管理员删除",
                    "work_deleted",
                    workId
            );
        } catch (Exception e) {
            log.warn("发送删除通知失败", e);
        }

        // 逻辑删除作品本身
        baseMapper.deleteById(workId);

        logService.recordAction("管理员删除作品", "作品", workId);
    }

    @Override
    public PageResult<WorkListVO> queryWorkList(WorkQueryRequest request) {
        // 1. 分页查询
        Page<Work> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Work> wrapper = Wrappers.<Work>lambdaQuery()
                .orderByDesc(Work::getUpdateTime);

        // 筛选条件
        if (request.getStatus() != null) {
            wrapper.eq(Work::getStatus, request.getStatus());
        }
        if (request.getParticipantUserId() != null) {
            appendParticipantScope(wrapper, request.getParticipantUserId());
        } else if (request.getSubmitterId() != null) {
            wrapper.eq(Work::getSubmitterId, request.getSubmitterId());
        }
        if (request.getBatchId() != null) {
            wrapper.eq(Work::getBatchId, request.getBatchId());
        }
        if (CollectionUtil.isNotEmpty(request.getExcludeWorkIds())) {
            wrapper.notIn(Work::getId, request.getExcludeWorkIds());
        }
        if (StrUtil.isNotBlank(request.getKeyword())) {
            wrapper.and(w -> w.like(Work::getTitle, request.getKeyword())
                    .or().like(Work::getSummary, request.getKeyword())
                    .or().like(Work::getTechStack, request.getKeyword())
                    .or().like(Work::getAdvisor, request.getKeyword()));
        }
        if (StrUtil.isNotBlank(request.getTechStack())) {
            wrapper.like(Work::getTechStack, request.getTechStack());
        }
        // 班级筛选：查询该班级下的所有用户，再按 submitterId 筛选
        if (request.getClassId() != null) {
            List<Long> userIds = sysUserMapper.selectList(
                    Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getClassId, request.getClassId())
                            .select(SysUser::getId))
                    .stream().map(SysUser::getId).collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                wrapper.in(Work::getSubmitterId, userIds);
            } else {
                // 无匹配用户，返回空
                return PageResult.of(List.of(), 0, request.getPageNum(), request.getPageSize());
            }
        }
        // 提交时间范围筛选
        if (StrUtil.isNotBlank(request.getSubmitTimeBegin())) {
            workQueryValidator.validateDateTime(request.getSubmitTimeBegin(), "submitTimeBegin");
            wrapper.ge(Work::getSubmitTime, request.getSubmitTimeBegin());
        }
        if (StrUtil.isNotBlank(request.getSubmitTimeEnd())) {
            workQueryValidator.validateDateTime(request.getSubmitTimeEnd(), "submitTimeEnd");
            wrapper.le(Work::getSubmitTime, request.getSubmitTimeEnd());
        }

        Page<Work> result = baseMapper.selectPage(page, wrapper);

        // 2. 转换为 VO
        List<WorkListVO> voList = result.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public WorkDetailVO getWorkDetail(Long id) {
        Work work = getWorkOrThrow(id);
        return buildWorkDetailVO(work);
    }

    @Override
    public WorkDetailVO getApprovedWorkDetail(Long id) {
        Work work = getWorkOrThrow(id);
        ensureApprovedForTeacherView(work);
        return buildWorkDetailVO(work);
    }

    @Override
    public WorkDetailVO getCurrentStudentWorkDetail(Long id) {
        Work work = getWorkOrThrow(id);
        checkOwnershipForView(work);
        return buildWorkDetailVO(work);
    }

    private WorkDetailVO buildWorkDetailVO(Work work) {
        Long id = work.getId();
        WorkDetailVO vo = new WorkDetailVO();
        vo.setId(work.getId());
        vo.setTitle(work.getTitle());
        vo.setSummary(work.getSummary());
        vo.setTechStack(work.getTechStack());
        vo.setAdvisor(work.getAdvisor());
        vo.setCoverUrl(work.getCoverUrl());
        vo.setVideoUrl(work.getVideoUrl());
        vo.setPreviewUrl(work.getPreviewUrl());
        vo.setRunDesc(work.getRunDesc());
        vo.setStatus(work.getStatus());
        vo.setStatusLabel(AuditStatusEnum.of(work.getStatus()).getLabel());
        vo.setSubmitterId(work.getSubmitterId());
        vo.setSubmitterName(resolveSubmitterName(work.getSubmitterId()));
        vo.setSubmitTime(work.getSubmitTime());
        vo.setBatchId(work.getBatchId());

        // 成员列表
        List<WorkMember> members = workMemberService.getByWorkId(id);
        vo.setMembers(members.stream().map(this::convertToMemberDTO).collect(Collectors.toList()));

        // 附件列表
        List<WorkAttachment> attachments = workAttachmentMapper.selectList(
                Wrappers.<WorkAttachment>lambdaQuery()
                        .eq(WorkAttachment::getWorkId, id));
        vo.setAttachments(attachments);

        // 发布信息
        WorkPublish publish = workPublishMapper.selectByWorkId(id);
        if (publish != null) {
            vo.setPublishStatus(publish.getPublishStatus());
            vo.setFeatured(publish.getFeatured());
        }

        // 评分与排行数据（仅排行榜已公示时显示）
        if (work.getBatchId() != null) {
            ScoreBatch batch = scoreBatchMapper.selectById(work.getBatchId());
            if (batch != null && Integer.valueOf(1).equals(batch.getRankPublished())) {
                var scoreSummary = scoreService.getScoreSummary(id);
                if (scoreSummary != null) {
                    vo.setAvgScore(scoreSummary.getAvgTotal() != null ? scoreSummary.getAvgTotal().toString() : null);
                    vo.setAvgInnovation(scoreSummary.getAvgInnovation() != null ? scoreSummary.getAvgInnovation().toString() : null);
                    vo.setAvgDifficulty(scoreSummary.getAvgDifficulty() != null ? scoreSummary.getAvgDifficulty().toString() : null);
                    vo.setAvgCompletion(scoreSummary.getAvgCompletion() != null ? scoreSummary.getAvgCompletion().toString() : null);
                    vo.setAvgPracticality(scoreSummary.getAvgPracticality() != null ? scoreSummary.getAvgPracticality().toString() : null);
                    vo.setTeacherCount(scoreSummary.getTeacherCount());
                }
                // 排名
                Integer rank = baseMapper.selectWorkRank(id, work.getBatchId());
                vo.setRank(rank);
            }
        }

        // 点赞数/浏览数
        vo.setLikeCount(work.getLikeCount() != null ? work.getLikeCount() : 0);
        vo.setViewCount(work.getViewCount() != null ? work.getViewCount() : 0);

        return vo;
    }

    private Work getWorkOrThrow(Long id) {
        Work work = baseMapper.selectById(id);
        if (work == null) {
            throw new BusinessException("作品不存在");
        }
        return work;
    }

    private void ensureApprovedForTeacherView(Work work) {
        if (work.getStatus() != AuditStatusEnum.APPROVED.getValue()) {
            throw new BusinessException("作品不存在");
        }
    }

    @Override
    public List<WorkListVO> getMyWorks(Long userId) {
        List<Work> list = listParticipatedWorks(userId);
        return list.stream().map(this::convertToListVO).collect(Collectors.toList());
    }

    @Override
    public List<Work> listParticipatedWorks(Long userId) {
        LambdaQueryWrapper<Work> wrapper = Wrappers.<Work>lambdaQuery()
                .orderByDesc(Work::getUpdateTime);
        appendParticipantScope(wrapper, userId);
        return baseMapper.selectList(wrapper);
    }

    // ==================== 私有方法 ====================

    /**
     * 根据 submitterId 查询提交人姓名
     */
    private String resolveSubmitterName(Long submitterId) {
        if (submitterId == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectById(submitterId);
        return user != null ? user.getRealName() : null;
    }

    /**
     * 追加”提交者或已注册成员”的参与范围
     */
    private void appendParticipantScope(LambdaQueryWrapper<Work> wrapper, Long userId) {
        if (userId == null) {
            return;
        }
        // 安全查询：先查 work_member 获取关联作品 ID，再传入 in() （避免 inSql 字符串拼接）
        List<Object> memberWorkIds = workMemberMapper.selectObjs(
                Wrappers.<WorkMember>lambdaQuery()
                        .select(WorkMember::getWorkId)
                        .eq(WorkMember::getStudentId, userId));
        wrapper.and(w -> {
            w.eq(Work::getSubmitterId, userId);
            if (!memberWorkIds.isEmpty()) {
                w.or().in(Work::getId, memberWorkIds);
            }
        });
    }

    /**
     * 校验当前用户是否为作品提交者
     */
    private void checkOwnership(Work work) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!Objects.equals(work.getSubmitterId(), currentUserId)) {
            throw new BusinessException("无权操作此作品");
        }
    }

    private String validatePreviewUrlForSubmission(String previewUrl) {
        if (StrUtil.isBlank(previewUrl)) {
            throw new BusinessException("提交审核前请填写服务器访问地址");
        }
        String normalizedPreviewUrl = normalizePreviewUrl(previewUrl);
        try {
            URI uri = new URI(normalizedPreviewUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new BusinessException("服务器访问地址仅支持IP、域名、http://或https://地址");
            }
            if (StrUtil.isBlank(host)) {
                throw new BusinessException("服务器访问地址必须包含有效的IP地址或域名");
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if ("localhost".equals(normalizedHost)
                    || "127.0.0.1".equals(normalizedHost)
                    || "0.0.0.0".equals(normalizedHost)
                    || "::1".equals(normalizedHost)) {
                throw new BusinessException("服务器访问地址不能使用本地地址");
            }
            return normalizedPreviewUrl;
        } catch (URISyntaxException e) {
            throw new BusinessException("服务器访问地址格式不正确");
        }
    }

    private String normalizePreviewUrl(String previewUrl) {
        if (StrUtil.isBlank(previewUrl)) {
            return previewUrl;
        }
        String normalized = previewUrl.trim();
        String lowerCaseUrl = normalized.toLowerCase(Locale.ROOT);
        if (lowerCaseUrl.startsWith("http://") || lowerCaseUrl.startsWith("https://")) {
            return normalized;
        }
        return "http://" + normalized;
    }

    private void ensureRequiredSubmissionFiles(Long workId) {
        List<WorkAttachment> attachments = workAttachmentMapper.selectList(
                Wrappers.<WorkAttachment>lambdaQuery()
                        .eq(WorkAttachment::getWorkId, workId));
        boolean hasVideo = attachments.stream().anyMatch(attachment -> "mp4".equals(normalizeFileType(attachment.getFileType())));
        if (!hasVideo) {
            throw new BusinessException("提交审核前请上传演示视频文件");
        }
    }

    private boolean isSourceArchive(String fileType) {
        String normalized = normalizeFileType(fileType);
        return "zip".equals(normalized) || "rar".equals(normalized) || "7z".equals(normalized);
    }

    private String normalizeFileType(String fileType) {
        return fileType == null ? "" : fileType.toLowerCase(Locale.ROOT);
    }

    private String resolveBoundMp4Url(Long workId) {
        return workAttachmentMapper.selectList(
                        Wrappers.<WorkAttachment>lambdaQuery()
                                .eq(WorkAttachment::getWorkId, workId))
                .stream()
                .filter(attachment -> "mp4".equals(normalizeFileType(attachment.getFileType())))
                .map(WorkAttachment::getFileUrl)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    /**
     * 校验当前学生是否可以查看作品详情
     */
    private void checkOwnershipForView(Work work) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!Objects.equals(work.getSubmitterId(), currentUserId) && !isRegisteredWorkMember(work.getId(), currentUserId)) {
            throw new BusinessException("无权查看此作品");
        }
    }

    /**
     * 校验当前学生是否为该作品的已注册团队成员
     */
    private boolean isRegisteredWorkMember(Long workId, Long userId) {
        if (workId == null || userId == null) {
            return false;
        }
        Long count = workMemberMapper.selectCount(
                Wrappers.<WorkMember>lambdaQuery()
                        .eq(WorkMember::getWorkId, workId)
                        .eq(WorkMember::getStudentId, userId));
        return count != null && count > 0;
    }

    /**
     * Work -> WorkListVO 转换
     */
    private WorkListVO convertToListVO(Work work) {
        WorkListVO vo = new WorkListVO();
        vo.setId(work.getId());
        vo.setTitle(work.getTitle());
        vo.setSummary(work.getSummary());
        vo.setTechStack(work.getTechStack());
        vo.setCoverUrl(work.getCoverUrl());
        vo.setPreviewUrl(work.getPreviewUrl());
        vo.setStatus(work.getStatus());
        vo.setStatusLabel(AuditStatusEnum.of(work.getStatus()).getLabel());
        vo.setSubmitterId(work.getSubmitterId());
        vo.setSubmitTime(work.getSubmitTime());
        vo.setSubmitterName(resolveSubmitterName(work.getSubmitterId()));

        // 批次信息
        vo.setBatchId(work.getBatchId());
        if (work.getBatchId() != null) {
            ScoreBatch batch = scoreBatchMapper.selectById(work.getBatchId());
            if (batch != null) {
                vo.setBatchName(batch.getBatchName());
            }
        }

        // 发布信息
        WorkPublish publish = workPublishMapper.selectByWorkId(work.getId());
        if (publish != null) {
            vo.setPublishStatus(publish.getPublishStatus());
            vo.setFeatured(publish.getFeatured());
        }

        // 团队成员数
        Long count = workMemberMapper.selectCount(
                Wrappers.<WorkMember>lambdaQuery()
                        .eq(WorkMember::getWorkId, work.getId()));
        vo.setMemberCount(count != null ? count.intValue() : 0);

        return vo;
    }

    /**
     * WorkMember -> WorkMemberDTO 转换
     */
    private WorkMemberDTO convertToMemberDTO(WorkMember member) {
        WorkMemberDTO dto = new WorkMemberDTO();
        dto.setId(member.getId());
        dto.setStudentId(member.getStudentId());
        dto.setStudentName(member.getStudentName());
        dto.setStudentNo(member.getStudentNo());
        dto.setClassName(member.getClassName());
        dto.setIsLeader(member.getIsLeader());
        // 补充用户头像
        if (member.getStudentId() != null) {
            SysUser user = sysUserMapper.selectById(member.getStudentId());
            if (user != null) {
                dto.setAvatar(user.getAvatar());
            }
        }
        return dto;
    }

    /**
     * 解析目标批次：优先使用前端指定的 batchId，未指定则取最新活跃批次
     */
    private ScoreBatch resolveTargetBatch(Long batchId) {
        if (batchId != null) {
            ScoreBatch batch = scoreBatchMapper.selectById(batchId);
            if (batch == null || batch.getDeleted() != 0) {
                throw new BusinessException("指定的评分批次不存在");
            }
            if (batch.getStatus() != 1) {
                throw new BusinessException("指定的评分批次已失效");
            }
            LocalDateTime now = LocalDateTime.now();
            if (batch.getStartTime() != null && now.isBefore(batch.getStartTime())) {
                throw new BusinessException("该评分批次尚未开始");
            }
            if (batch.getEndTime() != null && now.isAfter(batch.getEndTime())) {
                throw new BusinessException("该评分批次已截止");
            }
            return batch;
        }
        // 未指定 batchId 时取最新活跃批次（兼容非待办入口的创建）
        return scoreBatchMapper.selectOne(
                Wrappers.<ScoreBatch>lambdaQuery()
                        .eq(ScoreBatch::getStatus, 1)
                        .eq(ScoreBatch::getDeleted, 0)
                        .le(ScoreBatch::getStartTime, LocalDateTime.now())
                        .ge(ScoreBatch::getEndTime, LocalDateTime.now())
                        .orderByDesc(ScoreBatch::getCreateTime)
                        .last("LIMIT 1"));
    }
}

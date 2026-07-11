package com.jingxuan.modules.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkLike;
import com.jingxuan.entity.WorkPublish;
import com.jingxuan.entity.WorkTag;
import com.jingxuan.exception.NotFoundException;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.TagMapper;
import com.jingxuan.mapper.WorkLikeMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkMemberMapper;
import com.jingxuan.mapper.WorkPublishMapper;
import com.jingxuan.mapper.WorkTagMapper;
import com.jingxuan.modules.dict.service.DictService;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkListVO;
import com.jingxuan.modules.work.service.WorkService;
import com.jingxuan.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicWorkFacade {

    private final WorkService workService;
    private final WorkPublishMapper workPublishMapper;
    private final WorkMapper workMapper;
    private final WorkMemberMapper workMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final DictService dictService;
    private final WorkLikeMapper workLikeMapper;
    private final TagMapper tagMapper;
    private final WorkTagMapper workTagMapper;

    public PageResult<WorkListVO> getPublishedWorks(int page, int size, String keyword,
                                                    String techStack, Long classId,
                                                    List<Long> tagIds,
                                                    LocalDateTime submitTimeBegin,
                                                    LocalDateTime submitTimeEnd) {
        List<WorkPublish> published = workPublishMapper.selectList(
                Wrappers.<WorkPublish>lambdaQuery()
                        .eq(WorkPublish::getPublishStatus, 1));
        List<Long> workIds = published.stream()
                .map(WorkPublish::getWorkId)
                .collect(Collectors.toList());
        if (workIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }

        Map<Long, WorkPublish> publishMap = published.stream()
                .collect(Collectors.toMap(WorkPublish::getWorkId, p -> p));

        Page<Work> workPage = new Page<>(page, size);
        LambdaQueryWrapper<Work> wrapper = Wrappers.<Work>lambdaQuery()
                .in(Work::getId, workIds)
                .eq(Work::getStatus, 3)
                .orderByDesc(Work::getSubmitTime);
        applyPublishedWorkFilters(wrapper, keyword, techStack, classId, tagIds,
                submitTimeBegin, submitTimeEnd);

        Page<Work> result = workMapper.selectPage(workPage, wrapper);
        Long currentUserId = getCurrentUserId();
        List<WorkListVO> voList = result.getRecords().stream()
                .map(work -> toWorkListVO(work, publishMap.get(work.getId()), currentUserId))
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public List<SysDict> getPublicClasses() {
        return dictService.getByType("class");
    }

    public List<SysDict> getPublicTags() {
        return dictService.getByType("tech_stack");
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkDetailVO getPublishedWorkDetail(Long id) {
        WorkPublish publish = workPublishMapper.selectOne(
                Wrappers.<WorkPublish>lambdaQuery()
                        .eq(WorkPublish::getWorkId, id)
                        .eq(WorkPublish::getPublishStatus, 1));
        if (publish == null) {
            throw new NotFoundException("作品不存在或未发布");
        }
        WorkDetailVO vo = workService.getWorkDetail(id);

        Work work = workMapper.selectById(id);
        if (work != null) {
            vo.setLikeCount(work.getLikeCount() != null ? work.getLikeCount() : 0);
            vo.setViewCount(work.getViewCount() != null ? work.getViewCount() : 0);
        }
        vo.setTags(getWorkTagNames(id));

        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            vo.setLiked(isLiked(id, currentUserId));
        }

        workMapper.update(null, Wrappers.<Work>lambdaUpdate()
                .setSql("view_count = view_count + 1")
                .eq(Work::getId, id));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();

        Work work = workMapper.selectById(id);
        if (work == null) {
            throw new NotFoundException("作品不存在");
        }

        WorkLike existing = workLikeMapper.selectOne(
                Wrappers.<WorkLike>lambdaQuery()
                        .eq(WorkLike::getWorkId, id)
                        .eq(WorkLike::getUserId, userId));

        boolean liked;
        if (existing != null) {
            workLikeMapper.deleteById(existing.getId());
            workMapper.update(null, Wrappers.<Work>lambdaUpdate()
                    .setSql("like_count = GREATEST(like_count - 1, 0)")
                    .eq(Work::getId, id));
            liked = false;
        } else {
            WorkLike workLike = new WorkLike();
            workLike.setWorkId(id);
            workLike.setUserId(userId);
            workLikeMapper.insert(workLike);
            workMapper.update(null, Wrappers.<Work>lambdaUpdate()
                    .setSql("like_count = like_count + 1")
                    .eq(Work::getId, id));
            liked = true;
        }
        return likeResult(liked, getLikeCount(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void ensureLiked(Long id) {
        if (!Boolean.TRUE.equals((Boolean) toggleLikeWhenNeeded(id, true).get("liked"))) {
            throw new IllegalStateException("点赞状态设置失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void ensureUnliked(Long id) {
        toggleLikeWhenNeeded(id, false);
    }

    private Map<String, Object> toggleLikeWhenNeeded(Long id, boolean target) {
        Long userId = SecurityUtils.requireCurrentUserId();
        boolean current = isLiked(id, userId);
        if (current == target) return likeResult(current, getLikeCount(id));
        return toggleLike(id);
    }

    public Map<String, Object> getLikeStatus(Long id) {
        Long userId = getCurrentUserId();
        boolean liked = userId != null && isLiked(id, userId);
        return likeResult(liked, getLikeCount(id));
    }

    private void applyPublishedWorkFilters(LambdaQueryWrapper<Work> wrapper, String keyword,
                                           String techStack, Long classId, List<Long> tagIds,
                                           LocalDateTime submitTimeBegin,
                                           LocalDateTime submitTimeEnd) {
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Work::getTitle, keyword)
                    .or().like(Work::getSummary, keyword));
        }
        if (techStack != null && !techStack.isBlank()) {
            wrapper.like(Work::getTechStack, techStack);
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            List<Long> taggedWorkIds = workTagMapper.selectList(
                            Wrappers.<WorkTag>lambdaQuery().in(WorkTag::getTagId, tagIds))
                    .stream()
                    .map(WorkTag::getWorkId)
                    .distinct()
                    .collect(Collectors.toList());
            if (taggedWorkIds.isEmpty()) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(Work::getId, taggedWorkIds);
            }
        }
        if (classId != null) {
            List<Long> userIds = sysUserMapper.selectList(
                            Wrappers.<SysUser>lambdaQuery()
                                    .eq(SysUser::getClassId, classId)
                                    .select(SysUser::getId))
                    .stream()
                    .map(SysUser::getId)
                    .collect(Collectors.toList());
            if (userIds.isEmpty()) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(Work::getSubmitterId, userIds);
            }
        }
        if (submitTimeBegin != null) {
            wrapper.ge(Work::getSubmitTime, submitTimeBegin);
        }
        if (submitTimeEnd != null) {
            wrapper.le(Work::getSubmitTime, submitTimeEnd);
        }
    }

    private WorkListVO toWorkListVO(Work work, WorkPublish publish, Long currentUserId) {
        WorkListVO vo = new WorkListVO();
        vo.setId(work.getId());
        vo.setTitle(work.getTitle());
        vo.setSummary(work.getSummary());
        vo.setTechStack(work.getTechStack());
        vo.setCoverUrl(work.getCoverUrl());
        vo.setPreviewUrl(work.getPreviewUrl());
        vo.setStatus(work.getStatus());
        vo.setSubmitTime(work.getSubmitTime());
        if (publish != null) {
            vo.setPublishStatus(publish.getPublishStatus());
            vo.setFeatured(publish.getFeatured());
        }
        Long count = workMemberMapper.selectCount(
                Wrappers.<com.jingxuan.entity.WorkMember>lambdaQuery()
                        .eq(com.jingxuan.entity.WorkMember::getWorkId, work.getId()));
        vo.setMemberCount(count != null ? count.intValue() : 0);
        vo.setLikeCount(work.getLikeCount() != null ? work.getLikeCount() : 0);
        vo.setViewCount(work.getViewCount() != null ? work.getViewCount() : 0);
        vo.setTags(getWorkTagNames(work.getId()));
        if (currentUserId != null) {
            vo.setLiked(isLiked(work.getId(), currentUserId));
        }
        return vo;
    }

    private boolean isLiked(Long workId, Long userId) {
        return workLikeMapper.selectCount(
                Wrappers.<WorkLike>lambdaQuery()
                        .eq(WorkLike::getWorkId, workId)
                        .eq(WorkLike::getUserId, userId)) > 0;
    }

    private int getLikeCount(Long workId) {
        Work work = workMapper.selectById(workId);
        return work != null && work.getLikeCount() != null ? work.getLikeCount() : 0;
    }

    private Map<String, Object> likeResult(boolean liked, int likeCount) {
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", likeCount);
        return result;
    }

    private List<String> getWorkTagNames(Long workId) {
        List<WorkTag> workTags = workTagMapper.selectList(
                Wrappers.<WorkTag>lambdaQuery().eq(WorkTag::getWorkId, workId));
        if (workTags.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> tagIds = workTags.stream()
                .map(WorkTag::getTagId)
                .collect(Collectors.toList());
        List<com.jingxuan.entity.Tag> tags = tagMapper.selectBatchIds(tagIds);
        return tags.stream()
                .map(com.jingxuan.entity.Tag::getName)
                .collect(Collectors.toList());
    }

    private Long getCurrentUserId() {
        try {
            return SecurityUtils.requireCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}

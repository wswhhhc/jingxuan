package com.jingxuan.modules.work.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.entity.WorkAttachment;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.WorkAttachmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class WorkAttachmentBindingService {

    private final WorkAttachmentMapper workAttachmentMapper;

    void bindNewAttachments(Long workId, List<String> rawIds) {
        List<Long> attachmentIds = parseAttachmentIds(rawIds);
        log.debug("bindNewAttachments: raw attachmentIds={}, parsed attachmentIds={}", rawIds, attachmentIds);
        if (CollectionUtil.isEmpty(attachmentIds)) {
            return;
        }

        Long occupiedCount = workAttachmentMapper.selectCount(
                Wrappers.<WorkAttachment>lambdaQuery()
                        .in(WorkAttachment::getId, attachmentIds)
                        .isNotNull(WorkAttachment::getWorkId)
        );
        if (occupiedCount > 0) {
            throw new BusinessException("部分附件已被其他作品绑定，请重新上传");
        }

        workAttachmentMapper.update(
                null,
                Wrappers.<WorkAttachment>lambdaUpdate()
                        .set(WorkAttachment::getWorkId, workId)
                        .in(WorkAttachment::getId, attachmentIds)
                        .isNull(WorkAttachment::getWorkId)
        );
    }

    void replaceBindings(Long workId, List<String> rawTargetIds) {
        List<Long> targetIds = parseAttachmentIds(rawTargetIds);
        List<Long> currentIds = workAttachmentMapper.selectList(
                        Wrappers.<WorkAttachment>lambdaQuery()
                                .eq(WorkAttachment::getWorkId, workId)
                                .select(WorkAttachment::getId))
                .stream()
                .map(WorkAttachment::getId)
                .collect(Collectors.toList());

        List<Long> removedIds = currentIds.stream()
                .filter(currentId -> !targetIds.contains(currentId))
                .collect(Collectors.toList());
        List<Long> addedIds = targetIds.stream()
                .filter(targetId -> !currentIds.contains(targetId))
                .distinct()
                .collect(Collectors.toList());

        if (!addedIds.isEmpty()) {
            Long occupiedCount = workAttachmentMapper.selectCount(
                    Wrappers.<WorkAttachment>lambdaQuery()
                            .in(WorkAttachment::getId, addedIds)
                            .isNotNull(WorkAttachment::getWorkId)
                            .ne(WorkAttachment::getWorkId, workId)
            );
            if (occupiedCount > 0) {
                throw new BusinessException("部分附件已被其他作品绑定，请重新上传");
            }
        }

        if (!removedIds.isEmpty()) {
            workAttachmentMapper.update(
                    null,
                    Wrappers.<WorkAttachment>lambdaUpdate()
                            .set(WorkAttachment::getWorkId, null)
                            .in(WorkAttachment::getId, removedIds)
                            .eq(WorkAttachment::getWorkId, workId)
            );
        }

        if (!addedIds.isEmpty()) {
            workAttachmentMapper.update(
                    null,
                    Wrappers.<WorkAttachment>lambdaUpdate()
                            .set(WorkAttachment::getWorkId, workId)
                            .in(WorkAttachment::getId, addedIds)
                            .and(w -> w.isNull(WorkAttachment::getWorkId)
                                    .or().eq(WorkAttachment::getWorkId, workId))
            );
        }
    }

    void ensureHasBoundAttachment(Long workId) {
        Long attachmentCount = workAttachmentMapper.selectCount(
                Wrappers.<WorkAttachment>lambdaQuery()
                        .eq(WorkAttachment::getWorkId, workId));
        if (attachmentCount == null || attachmentCount == 0) {
            throw new BusinessException("请先上传附件再提交审核");
        }
    }

    void releaseAll(Long workId) {
        workAttachmentMapper.update(
                Wrappers.<WorkAttachment>lambdaUpdate()
                        .set(WorkAttachment::getWorkId, null)
                        .eq(WorkAttachment::getWorkId, workId)
        );
    }

    private List<Long> parseAttachmentIds(List<String> attachmentIds) {
        if (CollectionUtil.isEmpty(attachmentIds)) {
            return List.of();
        }
        List<Long> parsed = new ArrayList<>();
        for (String attachmentId : attachmentIds) {
            if (StrUtil.isBlank(attachmentId)) {
                continue;
            }
            try {
                parsed.add(Long.parseLong(attachmentId.trim()));
            } catch (NumberFormatException e) {
                throw new BusinessException("附件ID格式无效: " + attachmentId);
            }
        }
        return parsed;
    }
}

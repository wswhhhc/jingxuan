package com.jingxuan.referencedata.internal.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.entity.Tag;
import com.jingxuan.entity.WorkTag;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.exception.NotFoundException;
import com.jingxuan.mapper.TagMapper;
import com.jingxuan.mapper.WorkTagMapper;
import com.jingxuan.referencedata.api.V1DeletionImpact;
import com.jingxuan.referencedata.api.V1Tag;
import com.jingxuan.referencedata.api.V1TagRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 标签写入与物理删除用例。 */
@Service
public class ReferenceDataCommandService {

    private final TagMapper tagMapper;
    private final WorkTagMapper workTagMapper;

    public ReferenceDataCommandService(TagMapper tagMapper, WorkTagMapper workTagMapper) {
        this.tagMapper = tagMapper;
        this.workTagMapper = workTagMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public V1Tag createTag(V1TagRequest request) {
        Tag tag = new Tag();
        apply(tag, request);
        tagMapper.insert(tag);
        return new V1Tag(tag.getId().toString(), tag.getName(), tag.getColor(), tag.getType(), tag.getSort());
    }

    @Transactional(rollbackFor = Exception.class)
    public V1Tag updateTag(Long id, V1TagRequest request) {
        Tag tag = requiredTag(id);
        apply(tag, request);
        tagMapper.updateById(tag);
        return new V1Tag(tag.getId().toString(), tag.getName(), tag.getColor(), tag.getType(), tag.getSort());
    }

    public V1DeletionImpact tagImpact(Long id) {
        requiredTag(id);
        long references = workTagMapper.selectCount(Wrappers.<WorkTag>lambdaQuery().eq(WorkTag::getTagId, id));
        return new V1DeletionImpact("tag", id.toString(), references,
                references == 0 ? List.of() : List.of("work_tag: " + references));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id, boolean confirmed) {
        V1DeletionImpact impact = tagImpact(id);
        if (impact.referenceCount() > 0 && !confirmed) {
            throw new BusinessException(409, "标签仍被引用，请先确认删除影响");
        }
        workTagMapper.delete(Wrappers.<WorkTag>lambdaQuery().eq(WorkTag::getTagId, id));
        if (tagMapper.physicalDeleteById(id) != 1) {
            throw new NotFoundException("标签不存在");
        }
    }

    private Tag requiredTag(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new NotFoundException("标签不存在");
        }
        return tag;
    }

    private static void apply(Tag target, V1TagRequest source) {
        target.setName(source.name());
        target.setColor(source.color());
        target.setType(source.type());
        target.setSort(source.sort() == null ? 0 : source.sort());
    }
}

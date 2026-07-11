package com.jingxuan.modules.adapter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.entity.Tag;
import com.jingxuan.entity.WorkTag;
import com.jingxuan.mapper.TagMapper;
import com.jingxuan.mapper.WorkTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTagFacade {

    private final TagMapper tagMapper;
    private final WorkTagMapper workTagMapper;

    public List<Tag> listTags(String type) {
        return tagMapper.selectList(
                Wrappers.<Tag>lambdaQuery()
                        .eq(Tag::getDeleted, 0)
                        .eq(type != null && !type.isBlank(), Tag::getType, type)
                        .orderByAsc(Tag::getSort));
    }

    public void createTag(Tag tag) {
        tagMapper.insert(tag);
    }

    public void updateTag(Long id, Tag tag) {
        tag.setId(id);
        tagMapper.updateById(tag);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        tagMapper.deleteById(id);
        workTagMapper.delete(Wrappers.<WorkTag>lambdaQuery().eq(WorkTag::getTagId, id));
    }
}

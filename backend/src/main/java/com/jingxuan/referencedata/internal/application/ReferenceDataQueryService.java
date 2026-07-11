package com.jingxuan.referencedata.internal.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.entity.Tag;
import com.jingxuan.mapper.TagMapper;
import com.jingxuan.modules.dict.service.DictService;
import com.jingxuan.referencedata.api.V1ReferenceItem;
import com.jingxuan.referencedata.api.V1Tag;
import org.springframework.stereotype.Service;

import java.util.List;

/** 参考数据的只读查询用例。 */
@Service
public class ReferenceDataQueryService {

    private final DictService dictService;
    private final TagMapper tagMapper;

    public ReferenceDataQueryService(DictService dictService, TagMapper tagMapper) {
        this.dictService = dictService;
        this.tagMapper = tagMapper;
    }

    public List<V1ReferenceItem> dictionaries(String type) {
        return dictService.getByType(type).stream().map(V1ReferenceItem::from).toList();
    }

    public List<V1ReferenceItem> classes() {
        return dictionaries("class");
    }

    public List<V1Tag> tags(String type) {
        return tagMapper.selectList(Wrappers.<Tag>lambdaQuery()
                        .eq(Tag::getDeleted, 0)
                        .eq(type != null && !type.isBlank(), Tag::getType, type)
                        .orderByAsc(Tag::getSort))
                .stream().map(V1Tag::from).toList();
    }
}

package com.jingxuan.referencedata.internal.application;

import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.Tag;
import com.jingxuan.mapper.TagMapper;
import com.jingxuan.modules.dict.service.DictService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReferenceDataQueryServiceTest {

    private final DictService dictService = mock(DictService.class);
    private final TagMapper tagMapper = mock(TagMapper.class);
    private final ReferenceDataQueryService service = new ReferenceDataQueryService(dictService, tagMapper);

    @Test
    void classesAreReadFromClassDictionaryAndIdsAreStrings() {
        SysDict item = new SysDict();
        item.setId(100000000000000001L);
        item.setDictType("class");
        item.setDictLabel("软件 2401");
        when(dictService.getByType("class")).thenReturn(List.of(item));

        var classes = service.classes();

        assertEquals("100000000000000001", classes.get(0).id());
        assertEquals("软件 2401", classes.get(0).label());
    }

    @Test
    void tagsAreMappedToV1Shape() {
        Tag tag = new Tag();
        tag.setId(2L);
        tag.setName("Vue");
        tag.setType("technology");
        when(tagMapper.selectList(any())).thenReturn(List.of(tag));

        var tags = service.tags("technology");

        assertEquals("2", tags.get(0).id());
        assertEquals("Vue", tags.get(0).name());
    }
}

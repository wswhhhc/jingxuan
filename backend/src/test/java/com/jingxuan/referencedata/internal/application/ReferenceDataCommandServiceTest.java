package com.jingxuan.referencedata.internal.application;

import com.jingxuan.entity.Tag;
import com.jingxuan.mapper.TagMapper;
import com.jingxuan.mapper.WorkTagMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReferenceDataCommandServiceTest {

    private final TagMapper tagMapper = mock(TagMapper.class);
    private final WorkTagMapper workTagMapper = mock(WorkTagMapper.class);
    private final ReferenceDataCommandService service = new ReferenceDataCommandService(tagMapper, workTagMapper);

    @Test
    void impactListsWorkTagReferences() {
        when(tagMapper.selectById(9L)).thenReturn(tag(9L));
        when(workTagMapper.selectCount(any())).thenReturn(3L);

        var impact = service.tagImpact(9L);

        assertEquals(3L, impact.referenceCount());
        assertEquals("work_tag: 3", impact.references().get(0));
    }

    @Test
    void referencedTagRequiresExplicitConfirmation() {
        when(tagMapper.selectById(9L)).thenReturn(tag(9L));
        when(workTagMapper.selectCount(any())).thenReturn(1L);

        assertThrows(RuntimeException.class, () -> service.deleteTag(9L, false));

        verify(tagMapper, never()).physicalDeleteById(9L);
    }

    @Test
    void confirmedDeleteClearsLinksThenPhysicallyDeletesTag() {
        when(tagMapper.selectById(9L)).thenReturn(tag(9L));
        when(workTagMapper.selectCount(any())).thenReturn(1L);
        when(tagMapper.physicalDeleteById(9L)).thenReturn(1);

        service.deleteTag(9L, true);

        verify(workTagMapper).delete(any());
        verify(tagMapper).physicalDeleteById(9L);
    }

    private static Tag tag(Long id) {
        Tag tag = new Tag();
        tag.setId(id);
        return tag;
    }
}

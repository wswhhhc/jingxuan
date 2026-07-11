package com.jingxuan.portfolio.api;

import com.jingxuan.entity.WorkAttachment;
import com.jingxuan.modules.work.dto.WorkDetailVO;
import com.jingxuan.modules.work.dto.WorkMemberDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V1WorkDetailTest {

    @Test
    void mapsDetailUsingStringIdsOffsetTimeAndNestedFileMetadata() {
        WorkMemberDTO member = new WorkMemberDTO();
        member.setId(9007199254740994L);
        member.setStudentId(9007199254740995L);
        member.setStudentName("李同学");
        member.setStudentNo("20260001");
        member.setClassName("软件工程 1 班");
        member.setIsLeader(1);
        member.setAvatar("/uploads/avatar.png");
        WorkAttachment attachment = new WorkAttachment();
        attachment.setId(9007199254740996L);
        attachment.setFileName("source.zip");
        attachment.setFileType("application/zip");
        attachment.setFileSize(1024L);
        attachment.setFileUrl("/uploads/source.zip");
        attachment.setCategory("SOURCE");
        WorkDetailVO value = new WorkDetailVO();
        value.setId(9007199254740993L);
        value.setSubmitterId(7L);
        value.setBatchId(8L);
        value.setStatus(3);
        value.setPublishStatus(1);
        value.setFeatured(1);
        value.setSubmitTime(LocalDateTime.of(2026, 1, 1, 8, 0));
        value.setMembers(List.of(member));
        value.setAttachments(List.of(attachment));

        V1WorkDetail mapped = V1WorkDetail.from(value);

        assertEquals("9007199254740993", mapped.id());
        assertEquals("7", mapped.submitterId());
        assertEquals("8", mapped.batchId());
        assertEquals("APPROVED", mapped.status());
        assertEquals("PUBLISHED", mapped.publicationStatus());
        assertEquals("+08:00", mapped.submittedAt().substring(mapped.submittedAt().length() - 6));
        assertEquals("9007199254740994", mapped.members().get(0).id());
        assertEquals("9007199254740995", mapped.members().get(0).studentId());
        assertTrue(mapped.members().get(0).leader());
        assertEquals("9007199254740996", mapped.attachments().get(0).id());
        assertEquals("application/zip", mapped.attachments().get(0).contentType());
        assertTrue(mapped.featured());
    }

    @Test
    void mapsAbsentNestedCollectionsToEmptyAndUnpublishedStateSafely() {
        WorkDetailVO value = new WorkDetailVO();
        value.setStatus(0);
        value.setPublishStatus(0);
        value.setFeatured(0);

        V1WorkDetail mapped = V1WorkDetail.from(value);

        assertEquals("DRAFT", mapped.status());
        assertEquals("UNPUBLISHED", mapped.publicationStatus());
        assertTrue(mapped.members().isEmpty());
        assertTrue(mapped.attachments().isEmpty());
        assertFalse(mapped.featured());
    }

    @Test
    void masksMemberIdentityAndBatchForPublicShowcase() {
        WorkMemberDTO member = new WorkMemberDTO();
        member.setId(1L);
        member.setStudentId(2L);
        member.setStudentName("李同学");
        member.setStudentNo("20260001");
        member.setClassName("软件工程 1 班");
        member.setIsLeader(1);
        WorkDetailVO value = new WorkDetailVO();
        value.setSubmitterId(3L);
        value.setBatchId(4L);
        value.setMembers(List.of(member));

        V1WorkDetail mapped = V1WorkDetail.publicFrom(value);

        assertNull(mapped.submitterId());
        assertNull(mapped.batchId());
        assertEquals("李同学", mapped.members().get(0).name());
        assertNull(mapped.members().get(0).studentId());
        assertNull(mapped.members().get(0).studentNumber());
        assertNull(mapped.members().get(0).className());
        assertTrue(mapped.members().get(0).leader());
    }
}

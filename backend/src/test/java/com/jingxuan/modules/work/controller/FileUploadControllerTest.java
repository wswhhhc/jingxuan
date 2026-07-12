package com.jingxuan.modules.work.controller;

import com.jingxuan.mapper.WorkAttachmentMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.portfolio.api.FileStorage;
import com.jingxuan.entity.WorkAttachment;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileUploadControllerTest {

    @Test
    void removesStoredFileWhenAttachmentPersistenceFails() throws Exception {
        WorkAttachmentMapper attachmentMapper = mock(WorkAttachmentMapper.class);
        WorkMapper workMapper = mock(WorkMapper.class);
        FileStorage fileStorage = mock(FileStorage.class);
        when(fileStorage.store(eq("artifact.zip"), any()))
                .thenReturn(new FileStorage.StoredFile("2026-07-12/upload.zip", 3, "a".repeat(64)));
        doThrow(new RuntimeException("database unavailable")).when(attachmentMapper).insert(any(WorkAttachment.class));
        FileUploadController controller = new FileUploadController(attachmentMapper, workMapper, fileStorage);

        var result = controller.uploadFile(new MockMultipartFile("file", "artifact.zip", "application/zip", "zip".getBytes()), null);

        assertEquals(400, result.getCode());
        verify(fileStorage).delete("2026-07-12/upload.zip");
    }
}

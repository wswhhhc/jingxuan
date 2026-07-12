package com.jingxuan.portfolio.internal.infrastructure;

import com.jingxuan.portfolio.api.FileDeletionRequested;
import com.jingxuan.portfolio.api.FileStorage;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FileDeletionListenerTest {

    @Test
    void deletesTheStoredFileAfterReceivingTheCommittedEvent() throws Exception {
        FileStorage storage = mock(FileStorage.class);
        FileDeletionListener listener = new FileDeletionListener(storage);

        listener.on(new FileDeletionRequested("2026-07-12/file.zip"));

        verify(storage).delete("2026-07-12/file.zip");
    }

    @Test
    void propagatesStorageFailureSoPersistentEventPublicationCanRetry() throws Exception {
        FileStorage storage = mock(FileStorage.class);
        doThrow(new IOException("volume unavailable")).when(storage).delete("2026-07-12/file.zip");
        FileDeletionListener listener = new FileDeletionListener(storage);

        assertThrows(IllegalStateException.class,
                () -> listener.on(new FileDeletionRequested("2026-07-12/file.zip")));
    }
}

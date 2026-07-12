package com.jingxuan.portfolio.internal.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageTest {

    @TempDir
    Path directory;

    @Test
    void storesContentWithSha256AndDeletesItByRelativePath() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(directory.toString());

        var stored = storage.store("demo.txt", new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)));

        assertEquals(3, stored.size());
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", stored.sha256());
        Path storedPath = directory.resolve(stored.relativePath());
        assertTrue(Files.exists(storedPath));
        assertFalse(stored.relativePath().contains(".."));
        storage.delete(stored.relativePath());
        assertFalse(Files.exists(storedPath));
    }
}

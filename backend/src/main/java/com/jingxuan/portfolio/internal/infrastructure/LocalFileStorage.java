package com.jingxuan.portfolio.internal.infrastructure;

import com.jingxuan.portfolio.api.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

/** 本地文件系统适配器：临时写入、原子移动并流式计算 SHA-256。 */
@Service
public class LocalFileStorage implements FileStorage {
    private final Path root;

    public LocalFileStorage(@Value("${jingxuan.upload.path:./uploads}") String uploadPath) {
        this.root = Path.of(uploadPath).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(String originalName, InputStream content) throws IOException {
        String extension = extension(originalName);
        String relativePath = LocalDate.now() + "/" + UUID.randomUUID() + extension;
        Path target = resolve(relativePath);
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size;
            try (DigestInputStream input = new DigestInputStream(content, digest)) {
                size = Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            moveIntoPlace(temporary, target);
            return new StoredFile(relativePath.replace('\\', '/'), size, HexFormat.of().formatHex(digest.digest()));
        } catch (Exception exception) {
            Files.deleteIfExists(temporary);
            if (exception instanceof IOException ioException) throw ioException;
            throw new IOException("计算文件摘要失败", exception);
        }
    }

    @Override
    public void delete(String relativePath) throws IOException { Files.deleteIfExists(resolve(relativePath)); }

    private Path resolve(String relativePath) throws IOException {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) throw new IOException("非法文件路径");
        return resolved;
    }

    private static void moveIntoPlace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            // 部分挂载卷不支持原子移动；临时文件仍位于目标目录，因此回退不跨文件系统。
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String extension(String name) {
        int index = name == null ? -1 : name.lastIndexOf('.');
        return index >= 0 ? name.substring(index).toLowerCase(java.util.Locale.ROOT) : "";
    }
}

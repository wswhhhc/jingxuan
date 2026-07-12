package com.jingxuan.portfolio.api;

import java.io.IOException;
import java.io.InputStream;

/** 文件存储边界；业务层不依赖本地磁盘实现。 */
public interface FileStorage {

    StoredFile store(String originalName, InputStream content) throws IOException;

    void delete(String relativePath) throws IOException;

    record StoredFile(String relativePath, long size, String sha256) {
    }
}

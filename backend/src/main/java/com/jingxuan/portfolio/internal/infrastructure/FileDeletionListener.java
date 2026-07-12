package com.jingxuan.portfolio.internal.infrastructure;

import com.jingxuan.portfolio.api.FileDeletionRequested;
import com.jingxuan.portfolio.api.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 在业务事务提交后删除文件。
 *
 * <p>监听器失败时抛出异常，使 Spring Modulith 保留事件发布记录，由恢复任务重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileDeletionListener {

    static final String LISTENER_ID = "portfolio-file-deletion";

    private final FileStorage fileStorage;

    @ApplicationModuleListener(id = LISTENER_ID)
    void on(FileDeletionRequested event) {
        try {
            fileStorage.delete(event.relativePath());
        } catch (IOException exception) {
            log.warn("提交后的作品文件清理失败，将由持久化事件恢复任务重试（异常类型={}）。",
                    exception.getClass().getName());
            throw new IllegalStateException("作品文件清理失败", exception);
        }
    }
}

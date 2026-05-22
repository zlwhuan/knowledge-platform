package com.company.knowledge.service.impl;

import com.company.knowledge.service.PreviewCleanupService;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PreviewCleanupServiceImpl implements PreviewCleanupService {

    private static final Logger log = LoggerFactory.getLogger(PreviewCleanupServiceImpl.class);
    private static final Path PREVIEW_ROOT = Paths.get("previews");
    private static final Duration MAX_AGE = Duration.ofDays(30);

    @Override
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupStalePreviews() {
        if (!Files.isDirectory(PREVIEW_ROOT)) return;
        Instant cutoff = Instant.now().minus(MAX_AGE);
        int cleaned = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(PREVIEW_ROOT)) {
            for (Path entry : stream) {
                try {
                    FileTime lastModified = Files.getLastModifiedTime(entry);
                    if (lastModified.toInstant().isBefore(cutoff)) {
                        deleteRecursive(entry);
                        cleaned++;
                    }
                } catch (IOException e) {
                    log.warn("无法处理预览目录条目: {}", entry, e);
                }
            }
        } catch (IOException e) {
            log.warn("无法读取预览目录", e);
        }
        if (cleaned > 0) {
            log.info("预览清理完成，已清理 {} 个过期条目", cleaned);
        }
    }

    private void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path child : stream) {
                    deleteRecursive(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}

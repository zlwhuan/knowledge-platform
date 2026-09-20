package com.company.knowledge.util;

import com.company.knowledge.entity.Attachment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AttachmentPaths {

    public static final String UPLOAD_DIR = "uploads";

    private AttachmentPaths() {
    }

    public static String toRelative(String storedFileName) {
        return UPLOAD_DIR + "/" + storedFileName;
    }

    public static Path resolve(Attachment attachment) {
        String stored = attachment.getStoredFileName();
        if (stored != null && !stored.isBlank()) {
            Path underUploads = Paths.get(UPLOAD_DIR).resolve(stored);
            if (Files.exists(underUploads)) {
                return underUploads;
            }
        }

        String raw = attachment.getFilePath();
        if (raw == null || raw.isBlank()) {
            return Paths.get(UPLOAD_DIR).resolve(stored == null ? "" : stored);
        }

        Path path = Paths.get(raw);
        if (path.isAbsolute()) {
            if (Files.exists(path)) {
                return path;
            }
            String name = path.getFileName().toString();
            return Paths.get(UPLOAD_DIR).resolve(name);
        }
        return path;
    }
}

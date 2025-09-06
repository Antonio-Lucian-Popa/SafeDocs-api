package com.asusoftware.SafeDocs_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class StorageService {
    @Value("${app.storage.location}") private String storageRoot;

    public String save(MultipartFile file, UUID userId, UUID docId, int version, String sanitizedFilename) throws IOException {
        Path base = Paths.get(storageRoot, "u", userId.toString(), "d", docId.toString(), "v" + version);
        Files.createDirectories(base);
        Path dest = base.resolve(sanitizedFilename);
        file.transferTo(dest);
        return Paths.get("u", userId.toString(), "d", docId.toString(), "v" + version, sanitizedFilename).toString();
    }
}

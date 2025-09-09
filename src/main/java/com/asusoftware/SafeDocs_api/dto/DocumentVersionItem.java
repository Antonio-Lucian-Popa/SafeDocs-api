package com.asusoftware.SafeDocs_api.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentVersionItem(
        UUID id,
        Integer versionNo,
        String filePath,
        String mimeType,
        Long fileSize,
        Instant createdAt
) {}

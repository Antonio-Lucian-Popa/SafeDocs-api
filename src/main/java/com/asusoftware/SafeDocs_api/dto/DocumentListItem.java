package com.asusoftware.SafeDocs_api.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentListItem(
        UUID id,
        String title,
        UUID folderId,
        String mimeType,
        Long fileSize,
        Instant expiresAt,
        Instant createdAt
) {}

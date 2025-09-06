package com.asusoftware.SafeDocs_api.dto;


import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id, String title, UUID folderId, String filePath, String mimeType, Long fileSize, Instant expiresAt
) {}

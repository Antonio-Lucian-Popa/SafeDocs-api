package com.asusoftware.SafeDocs_api.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CreateDocumentRequest(
        @NotBlank String title,
        UUID folderId,
        Instant expiresAt,
        Map<String,Object> tags
) {}


package com.asusoftware.SafeDocs_api.dto;

import java.time.Instant;
import java.util.UUID;

public record ShareItemResponse(
        UUID id,
        UUID sharedWithUserId,
        String sharedWithEmail,
        String permission,
        Instant createdAt
) {}

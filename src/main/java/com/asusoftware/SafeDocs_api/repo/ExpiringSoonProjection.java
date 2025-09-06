package com.asusoftware.SafeDocs_api.repo;

import java.time.Instant;
import java.util.UUID;

public interface ExpiringSoonProjection {
    UUID getDocumentId();
    UUID getUserId();
    String getTitle();
    Instant getExpiresAt();
    Integer getDaysLeft();
}
package com.asusoftware.SafeDocs_api.dto;

import java.util.UUID;

public record ShareItem(UUID id, UUID folderId, String sharedWithEmail, String permission) {}
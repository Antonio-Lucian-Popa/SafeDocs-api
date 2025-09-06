package com.asusoftware.SafeDocs_api.dto;

import java.util.UUID;

public record FolderResponse(UUID id, String name, UUID parentId) {}

package com.asusoftware.SafeDocs_api.dto;


import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateFolderRequest(@NotBlank String name, UUID parentId) {}

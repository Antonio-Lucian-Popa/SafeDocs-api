package com.asusoftware.SafeDocs_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateShareRequest(
        @Email @NotBlank String targetEmail,
        @NotBlank String permission // "READ" | "WRITE"
) {}

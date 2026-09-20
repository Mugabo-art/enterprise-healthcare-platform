package com.healthplatform.auth.dto;

import com.healthplatform.auth.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotNull String email,
        @Size(min = 8, message = "Password must be at least 8 characters") @NotNull String password,
        @NotNull Role role
) {}

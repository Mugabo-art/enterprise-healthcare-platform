package com.healthplatform.auth.dto;

import jakarta.validation.constraints.NotNull;

public record RefreshRequest(@NotNull String refreshToken) {}

package com.healthplatform.auth.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MfaVerifyRequest(@NotNull UUID challengeId, @NotNull String code) {}

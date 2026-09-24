package com.healthplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(@NotBlank String challengeId, @NotBlank String code) {}

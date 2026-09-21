package com.healthplatform.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PrescriptionCreateRequest(
        @NotBlank String medicationName,
        @NotBlank String dosage,
        @NotBlank String frequency,
        @Positive Integer durationDays,
        String instructions
) {}

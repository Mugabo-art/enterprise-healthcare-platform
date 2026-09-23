package com.healthplatform.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MedicationUpdateRequest(
        @NotBlank String unit,
        @Min(0) int reorderThreshold,
        boolean active
) {}

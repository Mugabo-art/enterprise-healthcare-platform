package com.healthplatform.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MedicationCreateRequest(
        @NotBlank String name,
        @NotBlank String unit,
        @Min(0) int stockQuantity,
        @Min(0) int reorderThreshold
) {}

package com.healthplatform.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DispenseRequest(
        @NotNull UUID medicationId,
        @Min(1) int quantity
) {}

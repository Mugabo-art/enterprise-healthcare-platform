package com.healthplatform.visit.dto;

import com.healthplatform.visit.model.VisitType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record VisitCreateRequest(
        @NotNull Instant visitDate,
        @NotNull VisitType visitType,
        @NotNull String reason,
        String notes
) {}

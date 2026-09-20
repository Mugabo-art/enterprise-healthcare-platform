package com.healthplatform.medicalhistory.dto;

import com.healthplatform.medicalhistory.model.HistoryCategory;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MedicalHistoryCreateRequest(
        @NotNull HistoryCategory category,
        @NotNull String description,
        @NotNull LocalDate recordedDate
) {}

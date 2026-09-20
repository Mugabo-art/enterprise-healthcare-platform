package com.healthplatform.patient.dto;

import com.healthplatform.patient.model.Sex;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PatientCreateRequest(
        @NotNull String firstName,
        @NotNull String lastName,
        @NotNull LocalDate dateOfBirth,
        @NotNull Sex sex,
        String contactPhone,
        String contactEmail,
        String address
) {}

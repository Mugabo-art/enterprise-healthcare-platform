package com.healthplatform.lab.dto;

import jakarta.validation.constraints.NotBlank;

public record LabRequestCreateRequest(
        @NotBlank String testType,
        String notes
) {}

package com.healthplatform.lab.dto;

import com.healthplatform.lab.model.LabRequestStatus;
import jakarta.validation.constraints.NotNull;

public record LabRequestStatusUpdateRequest(
        @NotNull LabRequestStatus status
) {}

package com.healthplatform.visit.dto;

import com.healthplatform.visit.model.VisitStatus;

public record VisitUpdateRequest(
        String notes,
        String diagnosisCode,
        VisitStatus status
) {}

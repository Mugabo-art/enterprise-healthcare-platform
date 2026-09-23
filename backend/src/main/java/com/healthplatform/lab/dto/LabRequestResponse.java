package com.healthplatform.lab.dto;

import com.healthplatform.lab.model.LabRequest;
import com.healthplatform.lab.model.LabRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record LabRequestResponse(
        UUID id, UUID patientId, UUID visitId, UUID requestedById,
        String testType, LabRequestStatus status, String notes, Instant createdAt
) {
    public static LabRequestResponse from(LabRequest r) {
        return new LabRequestResponse(
                r.getId(), r.getPatient().getId(), r.getVisitId(), r.getRequestedById(),
                r.getTestType(), r.getStatus(), r.getNotes(), r.getCreatedAt()
        );
    }
}

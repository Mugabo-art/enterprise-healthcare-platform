package com.healthplatform.visit.dto;

import com.healthplatform.visit.model.Visit;
import com.healthplatform.visit.model.VisitType;

import java.time.Instant;
import java.util.UUID;

public record VisitResponse(
        UUID id, UUID patientId, Instant visitDate, VisitType visitType,
        String reason, String notes, UUID attendingStaffId, Instant createdAt
) {
    public static VisitResponse from(Visit v) {
        return new VisitResponse(
                v.getId(), v.getPatient().getId(), v.getVisitDate(), v.getVisitType(),
                v.getReason(), v.getNotes(), v.getAttendingStaffId(), v.getCreatedAt()
        );
    }
}

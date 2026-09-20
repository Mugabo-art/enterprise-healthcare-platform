package com.healthplatform.medicalhistory.dto;

import com.healthplatform.medicalhistory.model.HistoryCategory;
import com.healthplatform.medicalhistory.model.MedicalHistoryEntry;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MedicalHistoryResponse(
        UUID id, UUID patientId, HistoryCategory category, String description,
        LocalDate recordedDate, UUID recordedById, Instant createdAt
) {
    public static MedicalHistoryResponse from(MedicalHistoryEntry e) {
        return new MedicalHistoryResponse(
                e.getId(), e.getPatient().getId(), e.getCategory(), e.getDescription(),
                e.getRecordedDate(), e.getRecordedById(), e.getCreatedAt()
        );
    }
}

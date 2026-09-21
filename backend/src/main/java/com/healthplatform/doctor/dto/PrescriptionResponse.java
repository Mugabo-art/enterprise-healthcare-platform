package com.healthplatform.doctor.dto;

import com.healthplatform.doctor.model.Prescription;
import com.healthplatform.doctor.model.PrescriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record PrescriptionResponse(
        UUID id, UUID patientId, UUID visitId, UUID prescribedById,
        String medicationName, String dosage, String frequency, Integer durationDays,
        String instructions, PrescriptionStatus status, Instant createdAt
) {
    public static PrescriptionResponse from(Prescription p) {
        return new PrescriptionResponse(
                p.getId(), p.getPatient().getId(), p.getVisitId(), p.getPrescribedById(),
                p.getMedicationName(), p.getDosage(), p.getFrequency(), p.getDurationDays(),
                p.getInstructions(), p.getStatus(), p.getCreatedAt()
        );
    }
}

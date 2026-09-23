package com.healthplatform.pharmacy.dto;

import com.healthplatform.pharmacy.model.Dispensation;

import java.time.Instant;
import java.util.UUID;

public record DispensationResponse(
        UUID id, UUID patientId, UUID prescriptionId, UUID medicationId, String medicationName,
        int quantity, UUID dispensedById, Instant dispensedAt
) {
    public static DispensationResponse from(Dispensation d) {
        return new DispensationResponse(
                d.getId(), d.getPatientId(), d.getPrescriptionId(), d.getMedication().getId(),
                d.getMedication().getName(), d.getQuantity(), d.getDispensedById(), d.getDispensedAt()
        );
    }
}

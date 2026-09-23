package com.healthplatform.pharmacy.dto;

import com.healthplatform.pharmacy.model.Medication;

import java.util.UUID;

public record MedicationResponse(
        UUID id, String name, String unit, int stockQuantity, int reorderThreshold,
        boolean active, boolean lowStock
) {
    public static MedicationResponse from(Medication m) {
        return new MedicationResponse(
                m.getId(), m.getName(), m.getUnit(), m.getStockQuantity(), m.getReorderThreshold(),
                m.isActive(), m.isLowStock()
        );
    }
}

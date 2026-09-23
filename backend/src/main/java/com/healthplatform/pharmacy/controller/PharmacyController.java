package com.healthplatform.pharmacy.controller;

import com.healthplatform.auth.model.User;
import com.healthplatform.common.security.PatientAccessGuard;
import com.healthplatform.doctor.dto.PrescriptionResponse;
import com.healthplatform.pharmacy.dto.*;
import com.healthplatform.pharmacy.service.PharmacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Pharmacy", description = "Medication inventory and prescription dispensing")
public class PharmacyController {

    private final PharmacyService pharmacyService;
    private final PatientAccessGuard accessGuard;

    public PharmacyController(PharmacyService pharmacyService, PatientAccessGuard accessGuard) {
        this.pharmacyService = pharmacyService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/api/medications")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'DOCTOR')")
    @Operation(summary = "List medication inventory; lowStock=true returns only items at or below their reorder threshold")
    public ResponseEntity<List<MedicationResponse>> list(@RequestParam(defaultValue = "false") boolean lowStock) {
        return ResponseEntity.ok(pharmacyService.listMedications(lowStock));
    }

    @PostMapping("/api/medications")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    @Operation(summary = "Add a medication to the inventory")
    public ResponseEntity<MedicationResponse> create(@Valid @RequestBody MedicationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pharmacyService.createMedication(request));
    }

    @PutMapping("/api/medications/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    @Operation(summary = "Update a medication's unit, reorder threshold or active flag")
    public ResponseEntity<MedicationResponse> update(@PathVariable UUID id, @Valid @RequestBody MedicationUpdateRequest request) {
        return ResponseEntity.ok(pharmacyService.updateMedication(id, request));
    }

    @PostMapping("/api/medications/{id}/restock")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    @Operation(summary = "Add received stock to a medication")
    public ResponseEntity<MedicationResponse> restock(@PathVariable UUID id, @Valid @RequestBody RestockRequest request) {
        return ResponseEntity.ok(pharmacyService.restock(id, request));
    }

    @GetMapping("/api/pharmacy/prescriptions")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    @Operation(summary = "Work queue of active prescriptions awaiting dispensing, oldest first")
    public ResponseEntity<List<PrescriptionResponse>> pending() {
        return ResponseEntity.ok(pharmacyService.pendingPrescriptions());
    }

    @PostMapping("/api/prescriptions/{prescriptionId}/dispense")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    @Operation(summary = "Dispense a prescription: decrements stock and completes the prescription")
    public ResponseEntity<DispensationResponse> dispense(
            @PathVariable UUID prescriptionId,
            @Valid @RequestBody DispenseRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pharmacyService.dispense(prescriptionId, request, user.getId()));
    }

    @GetMapping("/api/patients/{patientId}/dispensations")
    @Operation(summary = "List a patient's dispensing history, newest first")
    public ResponseEntity<List<DispensationResponse>> listForPatient(@PathVariable UUID patientId, @AuthenticationPrincipal User user) {
        accessGuard.assertAccess(user, patientId);
        return ResponseEntity.ok(pharmacyService.listDispensations(patientId));
    }
}

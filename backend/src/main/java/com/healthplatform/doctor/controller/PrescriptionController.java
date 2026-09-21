package com.healthplatform.doctor.controller;

import com.healthplatform.auth.model.User;
import com.healthplatform.doctor.dto.PrescriptionCreateRequest;
import com.healthplatform.doctor.dto.PrescriptionResponse;
import com.healthplatform.doctor.service.PrescriptionService;
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
@Tag(name = "Prescriptions", description = "Medication orders issued by a doctor for a visit")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/api/patients/{patientId}/prescriptions")
    @Operation(summary = "List a patient's prescriptions, newest first")
    public ResponseEntity<List<PrescriptionResponse>> list(@PathVariable UUID patientId) {
        return ResponseEntity.ok(prescriptionService.list(patientId));
    }

    @PostMapping("/api/patients/{patientId}/visits/{visitId}/prescriptions")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Issue a prescription linked to a patient and visit")
    public ResponseEntity<PrescriptionResponse> create(
            @PathVariable UUID patientId,
            @PathVariable UUID visitId,
            @Valid @RequestBody PrescriptionCreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(prescriptionService.create(patientId, visitId, request, user.getId()));
    }
}

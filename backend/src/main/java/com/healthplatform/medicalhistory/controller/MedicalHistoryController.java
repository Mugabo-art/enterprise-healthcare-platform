package com.healthplatform.medicalhistory.controller;

import com.healthplatform.auth.model.User;
import com.healthplatform.common.security.PatientAccessGuard;
import com.healthplatform.medicalhistory.dto.MedicalHistoryCreateRequest;
import com.healthplatform.medicalhistory.dto.MedicalHistoryResponse;
import com.healthplatform.medicalhistory.service.MedicalHistoryService;
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
@RequestMapping("/api/patients/{patientId}/medical-history")
@Tag(name = "Medical History", description = "Patient allergies, conditions, medications, and other history entries")
public class MedicalHistoryController {

    private final MedicalHistoryService medicalHistoryService;
    private final PatientAccessGuard accessGuard;

    public MedicalHistoryController(MedicalHistoryService medicalHistoryService, PatientAccessGuard accessGuard) {
        this.medicalHistoryService = medicalHistoryService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    @Operation(summary = "List a patient's medical history, newest first")
    public ResponseEntity<List<MedicalHistoryResponse>> list(@PathVariable UUID patientId, @AuthenticationPrincipal User user) {
        accessGuard.assertAccess(user, patientId);
        return ResponseEntity.ok(medicalHistoryService.list(patientId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'DOCTOR')")
    @Operation(summary = "Add a medical history entry")
    public ResponseEntity<MedicalHistoryResponse> create(
            @PathVariable UUID patientId,
            @Valid @RequestBody MedicalHistoryCreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicalHistoryService.create(patientId, request, user.getId()));
    }
}

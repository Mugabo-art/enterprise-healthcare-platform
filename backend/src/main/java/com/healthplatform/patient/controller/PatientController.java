package com.healthplatform.patient.controller;

import com.healthplatform.auth.model.User;
import com.healthplatform.common.security.PatientAccessGuard;
import com.healthplatform.patient.dto.LinkUserRequest;
import com.healthplatform.patient.dto.PatientCreateRequest;
import com.healthplatform.patient.dto.PatientResponse;
import com.healthplatform.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Every write endpoint is role-gated via @PreAuthorize — default-deny, not
 * default-allow. See docs/THREAT_MODEL.md #11. Reads are gated per-record via
 * PatientAccessGuard so a PATIENT can only ever reach their own linked record.
 */
@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patients", description = "Patient records")
public class PatientController {

    private final PatientService patientService;
    private final PatientAccessGuard accessGuard;

    public PatientController(PatientService patientService, PatientAccessGuard accessGuard) {
        this.patientService = patientService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'DOCTOR', 'LAB_TECH', 'PHARMACIST', 'BILLING_CLERK')")
    @Operation(summary = "List patients, optionally filtered by last name (staff only)")
    public ResponseEntity<Page<PatientResponse>> list(
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        return ResponseEntity.ok(patientService.list(search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a patient by id")
    public ResponseEntity<PatientResponse> getById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        accessGuard.assertAccess(user, id);
        return ResponseEntity.ok(patientService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE')")
    @Operation(summary = "Create a patient")
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE')")
    @Operation(summary = "Update patient demographics")
    public ResponseEntity<PatientResponse> update(@PathVariable UUID id, @Valid @RequestBody PatientCreateRequest request) {
        return ResponseEntity.ok(patientService.update(id, request));
    }

    @PutMapping("/{id}/link-user")
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE')")
    @Operation(summary = "Link a self-registered PATIENT account to this patient record")
    public ResponseEntity<Void> linkUser(@PathVariable UUID id, @Valid @RequestBody LinkUserRequest request) {
        patientService.linkUser(id, request.userId());
        return ResponseEntity.noContent().build();
    }
}

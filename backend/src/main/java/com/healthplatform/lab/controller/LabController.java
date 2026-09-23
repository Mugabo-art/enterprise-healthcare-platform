package com.healthplatform.lab.controller;

import com.healthplatform.auth.model.User;
import com.healthplatform.common.security.PatientAccessGuard;
import com.healthplatform.lab.dto.*;
import com.healthplatform.lab.service.LabService;
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
@Tag(name = "Laboratory", description = "Test requests and results linked to a patient's visit")
public class LabController {

    private final LabService labService;
    private final PatientAccessGuard accessGuard;

    public LabController(LabService labService, PatientAccessGuard accessGuard) {
        this.labService = labService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/api/patients/{patientId}/lab-requests")
    @Operation(summary = "List a patient's lab requests, newest first")
    public ResponseEntity<List<LabRequestResponse>> list(@PathVariable UUID patientId, @AuthenticationPrincipal User user) {
        accessGuard.assertAccess(user, patientId);
        return ResponseEntity.ok(labService.list(patientId));
    }

    @PostMapping("/api/patients/{patientId}/visits/{visitId}/lab-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'DOCTOR')")
    @Operation(summary = "Request a lab test for a patient's visit")
    public ResponseEntity<LabRequestResponse> create(
            @PathVariable UUID patientId,
            @PathVariable UUID visitId,
            @Valid @RequestBody LabRequestCreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(labService.createRequest(patientId, visitId, request, user.getId()));
    }

    @PutMapping("/api/patients/{patientId}/lab-requests/{labRequestId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAB_TECH')")
    @Operation(summary = "Update a lab request's workflow status")
    public ResponseEntity<LabRequestResponse> updateStatus(
            @PathVariable UUID patientId,
            @PathVariable UUID labRequestId,
            @Valid @RequestBody LabRequestStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(labService.updateStatus(patientId, labRequestId, request));
    }

    @PostMapping("/api/patients/{patientId}/lab-requests/{labRequestId}/result")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAB_TECH')")
    @Operation(summary = "Record a result against a lab request")
    public ResponseEntity<LabResultResponse> recordResult(
            @PathVariable UUID patientId,
            @PathVariable UUID labRequestId,
            @Valid @RequestBody LabResultCreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(labService.recordResult(patientId, labRequestId, request, user.getId()));
    }

    @GetMapping("/api/patients/{patientId}/lab-requests/{labRequestId}/result")
    @Operation(summary = "Get the result recorded for a lab request")
    public ResponseEntity<LabResultResponse> getResult(
            @PathVariable UUID patientId,
            @PathVariable UUID labRequestId,
            @AuthenticationPrincipal User user
    ) {
        accessGuard.assertAccess(user, patientId);
        return ResponseEntity.ok(labService.getResult(patientId, labRequestId));
    }
}

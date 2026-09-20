package com.healthplatform.visit.controller;

import com.healthplatform.auth.model.User;
import com.healthplatform.visit.dto.VisitCreateRequest;
import com.healthplatform.visit.dto.VisitResponse;
import com.healthplatform.visit.service.VisitService;
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

/**
 * Unlike PatientController (ADMIN/NURSE only), DOCTOR can also write here —
 * recording a visit is a clinical action, not front-desk registration.
 */
@RestController
@RequestMapping("/api/patients/{patientId}/visits")
@Tag(name = "Visits", description = "Patient visit history")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @GetMapping
    @Operation(summary = "List a patient's visits, newest first")
    public ResponseEntity<List<VisitResponse>> list(@PathVariable UUID patientId) {
        return ResponseEntity.ok(visitService.list(patientId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'DOCTOR')")
    @Operation(summary = "Record a visit")
    public ResponseEntity<VisitResponse> create(
            @PathVariable UUID patientId,
            @Valid @RequestBody VisitCreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitService.create(patientId, request, user.getId()));
    }
}

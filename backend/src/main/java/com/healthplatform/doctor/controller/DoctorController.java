package com.healthplatform.doctor.controller;

import com.healthplatform.auth.model.Role;
import com.healthplatform.auth.model.User;
import com.healthplatform.common.exception.ApiException;
import com.healthplatform.visit.dto.VisitResponse;
import com.healthplatform.visit.service.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Reads visits via VisitService (the owning module's public API), never its
 * repository directly — see docs/ARCHITECTURE.md module boundary rules.
 */
@RestController
@RequestMapping("/api/doctors")
@Tag(name = "Doctors", description = "Doctor schedule")
public class DoctorController {

    private final VisitService visitService;

    public DoctorController(VisitService visitService) {
        this.visitService = visitService;
    }

    @GetMapping("/{doctorId}/visits")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "A doctor's schedule of visits (self, or any doctor for ADMIN)")
    public ResponseEntity<List<VisitResponse>> schedule(
            @PathVariable UUID doctorId,
            @AuthenticationPrincipal User user
    ) {
        if (user.getRole() == Role.DOCTOR && !user.getId().equals(doctorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Doctors may only view their own schedule");
        }
        return ResponseEntity.ok(visitService.findByDoctor(doctorId));
    }
}

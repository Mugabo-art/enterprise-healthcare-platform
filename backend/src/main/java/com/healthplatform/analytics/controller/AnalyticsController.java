package com.healthplatform.analytics.controller;

import com.healthplatform.analytics.dto.DashboardAnalyticsResponse;
import com.healthplatform.analytics.service.AnalyticsService;
import com.healthplatform.auth.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Hospital KPIs and dashboards")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'DOCTOR', 'LAB_TECH', 'PHARMACIST', 'BILLING_CLERK')")
    @Operation(summary = "KPI dashboard — hospital-wide, or a doctor's own patient load (staff only)")
    public ResponseEntity<DashboardAnalyticsResponse> dashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(analyticsService.getDashboard(user));
    }
}

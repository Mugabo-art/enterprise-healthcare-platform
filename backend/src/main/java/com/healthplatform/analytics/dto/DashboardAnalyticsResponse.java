package com.healthplatform.analytics.dto;

import java.util.List;

public record DashboardAnalyticsResponse(
        boolean selfScoped,
        long totalPatients,
        long newPatientsLast30Days,
        long totalVisits,
        List<LabelCount> visitsByStatus,
        List<LabelCount> visitsByType,
        List<DailyCount> visitsTrend,
        long totalPrescriptions,
        List<LabelCount> prescriptionsByStatus
) {}

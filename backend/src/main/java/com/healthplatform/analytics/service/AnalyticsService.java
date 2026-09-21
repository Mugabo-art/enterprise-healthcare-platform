package com.healthplatform.analytics.service;

import com.healthplatform.analytics.dto.DailyCount;
import com.healthplatform.analytics.dto.DashboardAnalyticsResponse;
import com.healthplatform.analytics.dto.LabelCount;
import com.healthplatform.auth.model.Role;
import com.healthplatform.auth.model.User;
import com.healthplatform.doctor.model.PrescriptionStatus;
import com.healthplatform.doctor.repository.PrescriptionRepository;
import com.healthplatform.patient.repository.PatientRepository;
import com.healthplatform.visit.model.Visit;
import com.healthplatform.visit.model.VisitStatus;
import com.healthplatform.visit.model.VisitType;
import com.healthplatform.visit.repository.VisitRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read-only aggregation across patient/visit/prescription data — no owned table
 * of its own. FR-7.2: a DOCTOR sees their own patient load, every other role
 * sees the hospital-wide view (doctorId == null means "no filter" in the
 * repository queries below).
 */
@Service
public class AnalyticsService {

    private static final int TREND_DAYS = 14;
    private static final int NEW_PATIENT_WINDOW_DAYS = 30;

    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;
    private final PrescriptionRepository prescriptionRepository;

    public AnalyticsService(PatientRepository patientRepository, VisitRepository visitRepository, PrescriptionRepository prescriptionRepository) {
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    public DashboardAnalyticsResponse getDashboard(User user) {
        boolean selfScoped = user.getRole() == Role.DOCTOR;
        UUID doctorId = selfScoped ? user.getId() : null;
        Instant newPatientCutoff = Instant.now().minus(NEW_PATIENT_WINDOW_DAYS, ChronoUnit.DAYS);

        long totalPatients = selfScoped
                ? visitRepository.countDistinctPatients(doctorId)
                : patientRepository.countByDeletedAtIsNull();

        long newPatients = selfScoped
                ? visitRepository.countDistinctPatientsSince(newPatientCutoff, doctorId)
                : patientRepository.countByDeletedAtIsNullAndCreatedAtAfter(newPatientCutoff);

        return new DashboardAnalyticsResponse(
                selfScoped,
                totalPatients,
                newPatients,
                visitRepository.countAll(doctorId),
                visitStatusBreakdown(doctorId),
                visitTypeBreakdown(doctorId),
                visitsTrend(doctorId),
                prescriptionRepository.countAll(doctorId),
                prescriptionStatusBreakdown(doctorId)
        );
    }

    private List<LabelCount> visitStatusBreakdown(UUID doctorId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (VisitStatus status : VisitStatus.values()) {
            counts.put(status.name(), 0L);
        }
        for (Object[] row : visitRepository.countGroupedByStatus(doctorId)) {
            counts.put(((VisitStatus) row[0]).name(), (Long) row[1]);
        }
        return toLabelCounts(counts);
    }

    private List<LabelCount> visitTypeBreakdown(UUID doctorId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (VisitType type : VisitType.values()) {
            counts.put(type.name(), 0L);
        }
        for (Object[] row : visitRepository.countGroupedByType(doctorId)) {
            counts.put(((VisitType) row[0]).name(), (Long) row[1]);
        }
        return toLabelCounts(counts);
    }

    private List<LabelCount> prescriptionStatusBreakdown(UUID doctorId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (PrescriptionStatus status : PrescriptionStatus.values()) {
            counts.put(status.name(), 0L);
        }
        for (Object[] row : prescriptionRepository.countGroupedByStatus(doctorId)) {
            counts.put(((PrescriptionStatus) row[0]).name(), (Long) row[1]);
        }
        return toLabelCounts(counts);
    }

    private List<DailyCount> visitsTrend(UUID doctorId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate windowStart = today.minusDays(TREND_DAYS - 1L);

        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (LocalDate d = windowStart; !d.isAfter(today); d = d.plusDays(1)) {
            counts.put(d, 0L);
        }

        for (Visit visit : visitRepository.findSince(windowStart.atStartOfDay().toInstant(ZoneOffset.UTC), doctorId)) {
            LocalDate day = LocalDate.ofInstant(visit.getVisitDate(), ZoneOffset.UTC);
            counts.merge(day, 1L, Long::sum);
        }

        return counts.entrySet().stream()
                .map(e -> new DailyCount(e.getKey(), e.getValue()))
                .toList();
    }

    private List<LabelCount> toLabelCounts(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .map(e -> new LabelCount(e.getKey(), e.getValue()))
                .toList();
    }
}

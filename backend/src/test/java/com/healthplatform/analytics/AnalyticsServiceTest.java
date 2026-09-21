package com.healthplatform.analytics;

import com.healthplatform.analytics.dto.DashboardAnalyticsResponse;
import com.healthplatform.analytics.dto.LabelCount;
import com.healthplatform.analytics.service.AnalyticsService;
import com.healthplatform.auth.model.Role;
import com.healthplatform.auth.model.User;
import com.healthplatform.doctor.model.PrescriptionStatus;
import com.healthplatform.doctor.repository.PrescriptionRepository;
import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.model.Sex;
import com.healthplatform.patient.repository.PatientRepository;
import com.healthplatform.visit.model.Visit;
import com.healthplatform.visit.model.VisitStatus;
import com.healthplatform.visit.model.VisitType;
import com.healthplatform.visit.repository.VisitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private PrescriptionRepository prescriptionRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User userWithRole(Role role) {
        return User.builder().id(UUID.randomUUID()).email("u@hospital.test").role(role).build();
    }

    @Test
    void getDashboard_isHospitalWideForNonDoctorRoles() {
        User admin = userWithRole(Role.ADMIN);

        when(patientRepository.countByDeletedAtIsNull()).thenReturn(42L);
        when(patientRepository.countByDeletedAtIsNullAndCreatedAtAfter(any())).thenReturn(5L);
        when(visitRepository.countAll(isNull())).thenReturn(100L);
        when(visitRepository.countGroupedByStatus(isNull())).thenReturn(List.<Object[]>of(new Object[]{VisitStatus.COMPLETED, 60L}));
        when(visitRepository.countGroupedByType(isNull())).thenReturn(List.<Object[]>of(new Object[]{VisitType.OUTPATIENT, 80L}));
        when(visitRepository.findSince(any(), isNull())).thenReturn(List.of());
        when(prescriptionRepository.countAll(isNull())).thenReturn(30L);
        when(prescriptionRepository.countGroupedByStatus(isNull())).thenReturn(List.<Object[]>of(new Object[]{PrescriptionStatus.ACTIVE, 20L}));

        DashboardAnalyticsResponse response = analyticsService.getDashboard(admin);

        assertFalse(response.selfScoped());
        assertEquals(42L, response.totalPatients());
        assertEquals(5L, response.newPatientsLast30Days());
        assertEquals(100L, response.totalVisits());
        assertEquals(30L, response.totalPrescriptions());
        assertTrue(response.visitsByStatus().contains(new LabelCount("COMPLETED", 60L)));
        assertTrue(response.visitsByStatus().contains(new LabelCount("SCHEDULED", 0L)));
        assertEquals(14, response.visitsTrend().size());
    }

    @Test
    void getDashboard_scopesToOwnPatientsForDoctorRole() {
        User doctor = userWithRole(Role.DOCTOR);

        when(visitRepository.countDistinctPatients(eq(doctor.getId()))).thenReturn(7L);
        when(visitRepository.countDistinctPatientsSince(any(), eq(doctor.getId()))).thenReturn(2L);
        when(visitRepository.countAll(eq(doctor.getId()))).thenReturn(15L);
        when(visitRepository.countGroupedByStatus(eq(doctor.getId()))).thenReturn(List.of());
        when(visitRepository.countGroupedByType(eq(doctor.getId()))).thenReturn(List.of());
        when(visitRepository.findSince(any(), eq(doctor.getId()))).thenReturn(List.of());
        when(prescriptionRepository.countAll(eq(doctor.getId()))).thenReturn(9L);
        when(prescriptionRepository.countGroupedByStatus(eq(doctor.getId()))).thenReturn(List.of());

        DashboardAnalyticsResponse response = analyticsService.getDashboard(doctor);

        assertTrue(response.selfScoped());
        assertEquals(7L, response.totalPatients());
        assertEquals(2L, response.newPatientsLast30Days());
        assertEquals(15L, response.totalVisits());
        assertEquals(9L, response.totalPrescriptions());
    }

    @Test
    void getDashboard_bucketsVisitsIntoDailyTrendCounts() {
        User admin = userWithRole(Role.ADMIN);
        Patient patient = Patient.builder().id(UUID.randomUUID()).firstName("A").lastName("B")
                .dateOfBirth(LocalDate.of(1990, 1, 1)).sex(Sex.MALE).build();
        Visit todayVisit = Visit.builder().id(UUID.randomUUID()).patient(patient)
                .visitDate(Instant.now()).visitType(VisitType.OUTPATIENT).reason("Checkup").build();

        when(patientRepository.countByDeletedAtIsNull()).thenReturn(1L);
        when(patientRepository.countByDeletedAtIsNullAndCreatedAtAfter(any())).thenReturn(1L);
        when(visitRepository.countAll(isNull())).thenReturn(1L);
        when(visitRepository.countGroupedByStatus(isNull())).thenReturn(List.of());
        when(visitRepository.countGroupedByType(isNull())).thenReturn(List.of());
        when(visitRepository.findSince(any(), isNull())).thenReturn(List.of(todayVisit));
        when(prescriptionRepository.countAll(isNull())).thenReturn(0L);
        when(prescriptionRepository.countGroupedByStatus(isNull())).thenReturn(List.of());

        DashboardAnalyticsResponse response = analyticsService.getDashboard(admin);

        long total = response.visitsTrend().stream().mapToLong(dc -> dc.count()).sum();
        assertEquals(1L, total);
        assertEquals(14, response.visitsTrend().size());
    }
}

package com.healthplatform.visit.repository;

import com.healthplatform.visit.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, UUID> {
    List<Visit> findByPatientIdOrderByVisitDateDesc(UUID patientId);

    List<Visit> findByAttendingStaffIdOrderByVisitDateDesc(UUID attendingStaffId);

    // doctorId == null means hospital-wide (no filter) — see AnalyticsService for the
    // DOCTOR-self-scoped vs. hospital-wide-view split (FR-7.2).
    @Query("SELECT COUNT(v) FROM Visit v WHERE (:doctorId IS NULL OR v.attendingStaffId = :doctorId)")
    long countAll(@Param("doctorId") UUID doctorId);

    @Query("SELECT COUNT(DISTINCT v.patient.id) FROM Visit v WHERE (:doctorId IS NULL OR v.attendingStaffId = :doctorId)")
    long countDistinctPatients(@Param("doctorId") UUID doctorId);

    @Query("SELECT COUNT(DISTINCT v.patient.id) FROM Visit v WHERE v.visitDate >= :since AND (:doctorId IS NULL OR v.attendingStaffId = :doctorId)")
    long countDistinctPatientsSince(@Param("since") Instant since, @Param("doctorId") UUID doctorId);

    @Query("SELECT v.status, COUNT(v) FROM Visit v WHERE (:doctorId IS NULL OR v.attendingStaffId = :doctorId) GROUP BY v.status")
    List<Object[]> countGroupedByStatus(@Param("doctorId") UUID doctorId);

    @Query("SELECT v.visitType, COUNT(v) FROM Visit v WHERE (:doctorId IS NULL OR v.attendingStaffId = :doctorId) GROUP BY v.visitType")
    List<Object[]> countGroupedByType(@Param("doctorId") UUID doctorId);

    @Query("SELECT v FROM Visit v WHERE v.visitDate >= :since AND (:doctorId IS NULL OR v.attendingStaffId = :doctorId)")
    List<Visit> findSince(@Param("since") Instant since, @Param("doctorId") UUID doctorId);
}

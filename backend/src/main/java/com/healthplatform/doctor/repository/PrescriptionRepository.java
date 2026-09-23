package com.healthplatform.doctor.repository;

import com.healthplatform.doctor.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    List<Prescription> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
    List<Prescription> findByStatusOrderByCreatedAtAsc(com.healthplatform.doctor.model.PrescriptionStatus status);

    // doctorId == null means hospital-wide (no filter) — see AnalyticsService.
    @Query("SELECT COUNT(p) FROM Prescription p WHERE (:doctorId IS NULL OR p.prescribedById = :doctorId)")
    long countAll(@Param("doctorId") UUID doctorId);

    @Query("SELECT p.status, COUNT(p) FROM Prescription p WHERE (:doctorId IS NULL OR p.prescribedById = :doctorId) GROUP BY p.status")
    List<Object[]> countGroupedByStatus(@Param("doctorId") UUID doctorId);
}

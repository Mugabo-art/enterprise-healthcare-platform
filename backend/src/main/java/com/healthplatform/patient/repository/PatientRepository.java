package com.healthplatform.patient.repository;

import com.healthplatform.patient.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Page<Patient> findByDeletedAtIsNullAndLastNameContainingIgnoreCase(String lastName, Pageable pageable);
    Page<Patient> findByDeletedAtIsNull(Pageable pageable);

    long countByDeletedAtIsNull();
    long countByDeletedAtIsNullAndCreatedAtAfter(Instant createdAfter);
}

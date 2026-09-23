package com.healthplatform.pharmacy.repository;

import com.healthplatform.pharmacy.model.Dispensation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DispensationRepository extends JpaRepository<Dispensation, UUID> {
    List<Dispensation> findByPatientIdOrderByDispensedAtDesc(UUID patientId);
}

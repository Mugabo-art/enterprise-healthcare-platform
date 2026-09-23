package com.healthplatform.pharmacy.repository;

import com.healthplatform.pharmacy.model.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MedicationRepository extends JpaRepository<Medication, UUID> {
    List<Medication> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}

package com.healthplatform.visit.repository;

import com.healthplatform.visit.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, UUID> {
    List<Visit> findByPatientIdOrderByVisitDateDesc(UUID patientId);
}

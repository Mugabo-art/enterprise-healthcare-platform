package com.healthplatform.lab.repository;

import com.healthplatform.lab.model.LabRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LabRequestRepository extends JpaRepository<LabRequest, UUID> {
    List<LabRequest> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}

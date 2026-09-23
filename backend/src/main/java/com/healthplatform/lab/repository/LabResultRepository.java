package com.healthplatform.lab.repository;

import com.healthplatform.lab.model.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LabResultRepository extends JpaRepository<LabResult, UUID> {
    Optional<LabResult> findByLabRequestId(UUID labRequestId);
}

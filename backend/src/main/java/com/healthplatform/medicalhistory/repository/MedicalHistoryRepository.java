package com.healthplatform.medicalhistory.repository;

import com.healthplatform.medicalhistory.model.MedicalHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MedicalHistoryRepository extends JpaRepository<MedicalHistoryEntry, UUID> {
    List<MedicalHistoryEntry> findByPatientIdOrderByRecordedDateDesc(UUID patientId);
}

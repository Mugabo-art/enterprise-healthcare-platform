package com.healthplatform.medicalhistory.service;

import com.healthplatform.common.exception.ApiException;
import com.healthplatform.medicalhistory.dto.MedicalHistoryCreateRequest;
import com.healthplatform.medicalhistory.dto.MedicalHistoryResponse;
import com.healthplatform.medicalhistory.model.MedicalHistoryEntry;
import com.healthplatform.medicalhistory.repository.MedicalHistoryRepository;
import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.repository.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MedicalHistoryService {

    private final MedicalHistoryRepository medicalHistoryRepository;
    private final PatientRepository patientRepository;

    public MedicalHistoryService(MedicalHistoryRepository medicalHistoryRepository, PatientRepository patientRepository) {
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional
    public MedicalHistoryResponse create(UUID patientId, MedicalHistoryCreateRequest request, UUID recordedById) {
        Patient patient = findActivePatient(patientId);

        MedicalHistoryEntry entry = MedicalHistoryEntry.builder()
                .patient(patient)
                .category(request.category())
                .description(request.description())
                .recordedDate(request.recordedDate())
                .recordedById(recordedById)
                .build();

        return MedicalHistoryResponse.from(medicalHistoryRepository.save(entry));
    }

    public List<MedicalHistoryResponse> list(UUID patientId) {
        findActivePatient(patientId);
        return medicalHistoryRepository.findByPatientIdOrderByRecordedDateDesc(patientId)
                .stream()
                .map(MedicalHistoryResponse::from)
                .toList();
    }

    private Patient findActivePatient(UUID patientId) {
        return patientRepository.findById(patientId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient not found"));
    }
}

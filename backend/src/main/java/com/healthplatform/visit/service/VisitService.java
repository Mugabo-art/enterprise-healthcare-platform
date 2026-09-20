package com.healthplatform.visit.service;

import com.healthplatform.common.exception.ApiException;
import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.repository.PatientRepository;
import com.healthplatform.visit.dto.VisitCreateRequest;
import com.healthplatform.visit.dto.VisitResponse;
import com.healthplatform.visit.model.Visit;
import com.healthplatform.visit.repository.VisitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VisitService {

    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;

    public VisitService(VisitRepository visitRepository, PatientRepository patientRepository) {
        this.visitRepository = visitRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional
    public VisitResponse create(UUID patientId, VisitCreateRequest request, UUID attendingStaffId) {
        Patient patient = findActivePatient(patientId);

        Visit visit = Visit.builder()
                .patient(patient)
                .visitDate(request.visitDate())
                .visitType(request.visitType())
                .reason(request.reason())
                .notes(request.notes())
                .attendingStaffId(attendingStaffId)
                .build();

        return VisitResponse.from(visitRepository.save(visit));
    }

    public List<VisitResponse> list(UUID patientId) {
        findActivePatient(patientId);
        return visitRepository.findByPatientIdOrderByVisitDateDesc(patientId)
                .stream()
                .map(VisitResponse::from)
                .toList();
    }

    private Patient findActivePatient(UUID patientId) {
        return patientRepository.findById(patientId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient not found"));
    }
}
